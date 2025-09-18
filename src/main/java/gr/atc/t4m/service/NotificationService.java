package gr.atc.t4m.service;

import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.dto.UserDto;
import gr.atc.t4m.dto.UserManagerResponse;
import gr.atc.t4m.enums.NotificationStatus;
import gr.atc.t4m.model.Notification;
import gr.atc.t4m.repository.NotificationRepository;
import gr.atc.t4m.service.interfaces.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static gr.atc.t4m.exception.CustomExceptions.*;

import java.util.*;

@Service
@Slf4j
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;

    private final ModelMapper modelMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.token-uri}")
    private String tokenUri;

    @Value("${user.manager.component.url}")
    private String userManagerUrl;

    @Value("${keycloak.client}")
    private String client;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    private static final String MAPPING_EXCEPTION = "Unable to convert Notification model to DTO";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String TOKEN = "access_token";
    private static final String JWT_ERROR = "Unable to retrieve Component's JWT Token - Client credentials";

    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Delete notification by ID
     *
     * @param notificationId : ID of the notification to be deleted
     * @throws ResourceNotFoundException : if the notification with the given ID is not found
     * @throws ForbiddenAccessException : if the user is not allowed to delete this notification
     */
    @Override
    public void deleteNotificationById(Long notificationId, String userId) {
        validateAndGetNotification(notificationId, userId);

        notificationRepository.deleteById(notificationId);
    }

    /**
     * Update Notification Status to 'READ'
     *
     * @param notificationId : ID of the notification to be updated
     * @throws ResourceNotFoundException : if the notification with the given ID is not found
     * @throws ForbiddenAccessException : if the user is not allowed to update this notification
     */
    @Override
    public void updateNotificationStatusToRead(Long notificationId, String userId) {
        Notification notification = validateAndGetNotification(notificationId, userId);

        notification.setNotificationStatus(NotificationStatus.READ.toString());
        notificationRepository.save(notification);
    }

    /**
     * Retrieve all Notifications for a specific User ID
     *
     * @param userId   : User ID
     * @param pageable : pagination parameters
     * @return Page<NotificationDto> : Page of notifications
     */
    @Override
    public Page<NotificationDto> retrieveAllNotificationsPerUserId(String userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);

        List<NotificationDto> notificationDtos = convertPageOfNotificationsToListOfDto(notifications);

        return new PageImpl<>(notificationDtos, pageable, notifications.getTotalElements());
    }


    /**
     * Retrieve all Unread Notifications for a specific User ID
     *
     * @param userId   : User ID
     * @param pageable : pagination parameters
     * @return Page<NotificationDto> : Page of notifications
     */
    @Override
    public Page<NotificationDto> retrieveUnreadNotificationsPerUserId(String userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdAndNotificationStatus(userId, NotificationStatus.UNREAD.toString(), pageable);

        List<NotificationDto> notificationDtos = convertPageOfNotificationsToListOfDto(notifications);

        return new PageImpl<>(notificationDtos, pageable, notifications.getTotalElements());

    }

    /*
     * Helper method to convert a Page of Notifications to a List of NotificationDtos
     */
    List<NotificationDto> convertPageOfNotificationsToListOfDto(Page<Notification> notifications){
        return notifications.getContent()
                .stream()
                .map(notification -> modelMapper.map(notification, NotificationDto.class))
                .toList();
    }

    /**
     * Retrieve Notification by ID
     *
     * @param notificationId : ID of the notification to be retrieved
     * @throws ResourceNotFoundException : if the notification with the given ID is not found
     * @throws ForbiddenAccessException : if the user is not allowed to update this notification
     * @return NotificationDto
     */
    @Override
    public NotificationDto retrieveNotificationById(Long notificationId, String userId) {
        Notification notification = validateAndGetNotification(notificationId, userId);

        try {
            return modelMapper.map(notification, NotificationDto.class);
        } catch (MappingException e) {
            log.error(MAPPING_EXCEPTION + " for notification: {}", notificationId);
            throw new ModelMappingException(MAPPING_EXCEPTION);
        }
    }

    /**
     * Create Notifications for each User
     *
     * @param users : List of Users
     * @param notification : Notification to be created
     */
    @Override
    public void createNotificationsForEachUser(List<UserDto> users, NotificationDto notification) {
        try {
            List<Notification> notificationToSave = new ArrayList<>();
            for (UserDto user : users) {
                Notification newNotification = modelMapper.map(notification, Notification.class);
                newNotification.setUserId(user.userId());
                newNotification.setUser(user.firstName() + " " + user.lastName());

                notificationToSave.add(newNotification);
            }

            // Include SUPER_ADMIN role in the Notification
            Notification newNotification = modelMapper.map(notification, Notification.class);
            newNotification.setUserId(SUPER_ADMIN_ROLE);
            newNotification.setUser("SUPER_ADMIN");
            notificationToSave.add(newNotification);


            notificationRepository.saveAll(notificationToSave);
        } catch (MappingException e) {
            log.error(MAPPING_EXCEPTION);
        }

    }

    /**
     * Retrieve Users for a specific Organization
     *
     * @param organization : Organization
     * @return List<UserDto>
     */
    // As the Kafka Event message reaches both Notif. Service and User Manager, the latter should first create the Organization
    // And Then the Notification Service request the resource. Thus, we introduce here a retry mechanism only for 404 errors
    @Retryable(
            value = { ResourceNotFoundException.class }, // retry only on 404
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // exponential backoff
    )
    @Override
    public List<UserDto> retrieveUserIdsPerOrganization(String organization) {
        // Retrieve Component's JWT Token - Client credentials
        String token = retrieveComponentJwtToken();
        if (token == null){
            log.error(JWT_ERROR);
            return Collections.emptyList();
        }

        // Retrieve User Ids
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            // Retrieve User Ids per role
            String requestUri = userManagerUrl.concat("/api/users/pilots/").concat(organization);
            ResponseEntity<UserManagerResponse> response = restTemplate.exchange(
                    requestUri,
                    HttpMethod.GET,
                    entity,
                    UserManagerResponse.class
            );

            log.info("Response: {}", response.getBody());
            return Optional.of(response)
                    .filter(resp -> resp.getStatusCode().is2xxSuccessful())
                    .map(ResponseEntity::getBody)
                    .map(UserManagerResponse::getData)
                    .orElse(Collections.emptyList());
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 404) {
                    throw new ResourceNotFoundException("Organization not found yet in Keycloak..");
                }
            }

            log.error("Unable to retrieve users for organization {} -  Error: {}", organization, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve Users for a Set of UserRoles within an Organization
     *
     * @param userRoles : Set of User Roles
     * @param organization : Organization
     * @return List<UserDto>
     */
    // As the Kafka Event message reaches both Notif. Service and User Manager, the latter should first create the Organization
    // And Then the Notification Service request the resource. Thus, we introduce here a retry mechanism only for 404 errors
    @Retryable(
            value = { ResourceNotFoundException.class }, // retry only on 404
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // exponential backoff
    )
    @Override
    public List<UserDto> retrieveUserIdsPerUserRolesAndOrganization(Set<String> userRoles, String organization) {
        // Retrieve Component's JWT Token - Client credentials
        String token = retrieveComponentJwtToken();
        if (token == null){
            return Collections.emptyList();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        // Retrieve User Ids per role
        List<UserDto> allUsers = new ArrayList<>();
        userRoles.forEach(role -> {
            try {
                ResponseEntity<UserManagerResponse> response = restTemplate.exchange(
                        userManagerUrl.concat("/api/users/pilots/").concat(organization).concat("/roles/").concat(role),
                        HttpMethod.GET,
                        entity,
                        UserManagerResponse.class
                );

                // Parse response and retrieve user Ids
                List<UserDto> users = Optional.of(response)
                        .filter(resp -> resp.getStatusCode().is2xxSuccessful())
                        .map(ResponseEntity::getBody)
                        .map(UserManagerResponse::getData)
                        .orElse(Collections.emptyList());

                allUsers.addAll(users);
            } catch (RestClientException e) {
                if (e instanceof HttpClientErrorException ex) {
                    if (ex.getStatusCode().value() == 404) {
                        throw new ResourceNotFoundException("Organization or Role not found yet in Keycloak..");
                    }
                }
                log.error("Unable to locate Users for Role: {} - Error: {}", role, e.getMessage());
            }
        });
        return allUsers;
    }

    /*
     * Helper method to validate the access to a notification and whether it exists
     */
    private Notification validateAndGetNotification(Long notificationId, String userId) {
        Optional<Notification> existingNotification = notificationRepository.findById(notificationId);

        if (existingNotification.isEmpty()) {
            throw new ResourceNotFoundException("Notification with id " + notificationId + " not found");
        }

        Notification notification = existingNotification.get();

        if (!notification.getUserId().equals(userId) && !SUPER_ADMIN_ROLE.equals(userId)) {
            throw new ForbiddenAccessException("You are not allowed to access this notification");
        }

        return notification;
    }

    /**
     * Generate a JWT Token to access Keycloak resources
     *
     * @return Token
     */
    String retrieveComponentJwtToken(){
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", client);
            map.add("client_secret", clientSecret);
            map.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return Optional.of(response)
                    .filter(resp -> resp.getStatusCode().is2xxSuccessful())
                    .map(ResponseEntity::getBody)
                    .filter(body -> body.get(TOKEN) != null)
                    .map(body -> body.get(TOKEN).toString())
                    .orElse(null);
        }  catch (RestClientException e) {
            log.error("Rest Client error during authenticating the client: Error: {}", e.getMessage());
            return null;
        }
    }
}
