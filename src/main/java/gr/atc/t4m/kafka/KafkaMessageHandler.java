package gr.atc.t4m.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.config.properties.KafkaProperties;
import gr.atc.t4m.dto.EventDto;
import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.dto.UserDto;
import gr.atc.t4m.enums.NotificationStatus;
import gr.atc.t4m.enums.Priority;
import gr.atc.t4m.service.interfaces.IEventMappingService;
import gr.atc.t4m.service.interfaces.INotificationService;
import gr.atc.t4m.service.interfaces.IWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import static gr.atc.t4m.exception.CustomExceptions.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class KafkaMessageHandler {

    private static final String GLOBAL_EVENT_MAPPINGS = "ALL";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final INotificationService notificationService;

    private final IEventMappingService eventMappingService;

    private final IWebSocketService webSocketService;

    private final ObjectMapper objectMapper;

    public KafkaMessageHandler(INotificationService notificationService, IEventMappingService eventMappingService, ObjectMapper objectMapper, KafkaProperties kafkaProperties, IWebSocketService webSocketService) {
        this.notificationService = notificationService;
        this.eventMappingService = eventMappingService;
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
        log.info("Kafka consumer initialized to listen to topics: {}", String.join(", ", kafkaProperties.consumer().getTopicsList()));
    }

    /**
     * Kafka consumer method to receive a JSON Event message - From Kafka Producers
     *
     * @param event: Event occurred in MODAPTO
     * @param topic: The topic from which the message was received
     */
    @KafkaListener(topics = "#{'${spring.kafka.consumer.topics}'.split(',')}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        // Validate that same essential variables are present
        if (!isValidEvent(event)) {
            log.error("Kafka message error - Missing essential variables. Message is discarded! Data: {}", event);
            return;
        }

        log.info("Event Received: {}", event);
        try {
            // Retrieve User Roles from Mappings (if exist)
            Set<String> userRolesPerEventType = new HashSet<>();
            try {
                userRolesPerEventType = eventMappingService.retrieveEventMappingByTopic(topic).getUserRoles();
            } catch (ResourceNotFoundException e){
                log.info("{}-Will create default event mapping", e.getMessage());
            }

            // Locate Related User IDs
            List<UserDto> users = determineRecipientsOfNotification(event, userRolesPerEventType, topic);

            // Generate Notification
            NotificationDto eventNotification = generateNotificationFromEvent(event);
            log.info("Notification created: {}", eventNotification);

            // Store notifications per each User - Async
            notificationService.createNotificationsForEachUser(users, eventNotification);

            String notificationMessage = objectMapper.writeValueAsString(eventNotification);
            if (userRolesPerEventType.isEmpty() || userRolesPerEventType.contains(GLOBAL_EVENT_MAPPINGS))
                // Send notification globally to pilot users
                webSocketService.notifyUsersAndRolesViaWebSocket(notificationMessage, event.organization().toUpperCase());
            else
                // Send notification through WebSockets to all user roles in the plant
                userRolesPerEventType.forEach(role -> webSocketService.notifyUsersAndRolesViaWebSocket(notificationMessage, role));

            // Send notification through WebSockets for Super-Admins
            webSocketService.notifyUsersAndRolesViaWebSocket(notificationMessage, SUPER_ADMIN_ROLE);
        } catch (ModelMappingException e) {
            log.error("An internal mapping exception occurred - Error: {}", e.getMessage());
        } catch (JsonProcessingException e) {
                log.error("Unable to convert Notification to string message - {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unable to process Notification from Event - {}", e.getMessage());
        }
    }

    /*
     * Helper method to locate the UserIDs that will receive the Notification
     */
    private List<UserDto> determineRecipientsOfNotification(EventDto event, Set<String> userRolesPerEventType, String topic) {
        List<UserDto> relatedUsers = new ArrayList<>();

        String formattedOrganization = event.organization().trim().toUpperCase();

        // Handle empty mappings case - Creating mapping and retrieve all pilot users
        if (userRolesPerEventType.isEmpty()) {
            log.info("No mappings exist for topic '{}'. All users in {} organization will be informed!", topic, formattedOrganization);

            // Request creation of mapping - Async
            eventMappingService.createDefaultNotificationMappingAsync(topic);

            relatedUsers.addAll(notificationService.retrieveUserIdsPerOrganization(formattedOrganization));
        } else if (userRolesPerEventType.contains(GLOBAL_EVENT_MAPPINGS)) { // Handle "ALL" role mapping - Send notification globally to pilot users
            relatedUsers.addAll(notificationService.retrieveUserIdsPerOrganization(formattedOrganization));
        } else { // Handle specific roles
            relatedUsers.addAll(notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRolesPerEventType, formattedOrganization));
        }

        return relatedUsers;
    }


    /*
     * Helper method to generate a Notification from Event
     */
    private NotificationDto generateNotificationFromEvent(EventDto event) {
        return NotificationDto.builder()
                .notificationStatus(NotificationStatus.UNREAD.toString())
                .sourceComponent(event.sourceComponent())
                .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC))
                .priority(event.priority())
                .description(event.description())
                .type(event.type())
                .user(null)
                .userId(null)
                .build();
    }

    /*
     * Helper method to validate that an Event has all the essential variables
     */
    private boolean isValidEvent(EventDto event) {
        return (event.priority() != null && EnumUtils.isValidEnumIgnoreCase(Priority.class, event.priority())) &&
                event.sourceComponent() != null &&
                event.organization() != null &&
                event.type() != null;
    }
}
