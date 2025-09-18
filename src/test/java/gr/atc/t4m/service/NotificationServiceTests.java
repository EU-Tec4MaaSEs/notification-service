package gr.atc.t4m.service;

import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.dto.UserDto;
import gr.atc.t4m.dto.UserManagerResponse;
import gr.atc.t4m.enums.NotificationStatus;
import gr.atc.t4m.enums.Priority;
import gr.atc.t4m.model.Notification;
import gr.atc.t4m.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.ErrorMessage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static gr.atc.t4m.exception.CustomExceptions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
class NotificationServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private NotificationDto testNotificationDto;
    private final String TEST_USER_ID = "user123";
    private final String SUPER_ADMIN_USER_ID = "SUPER_ADMIN";
    private final String OTHER_USER_ID = "otherUser";
    private final Long TEST_NOTIFICATION_ID = 1L;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(TEST_NOTIFICATION_ID);
        testNotification.setUserId(TEST_USER_ID);
        testNotification.setUser("Test User");
        testNotification.setDescription("Test Description");
        testNotification.setNotificationStatus(NotificationStatus.UNREAD.toString());
        testNotification.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        testNotification.setSourceComponent("Test Component");
        testNotification.setPriority(Priority.MID.toString());

        testNotificationDto = new NotificationDto();
        testNotificationDto.setId(TEST_NOTIFICATION_ID);
        testNotificationDto.setUserId(TEST_USER_ID);
        testNotificationDto.setUser("Test User");
        testNotificationDto.setDescription("Test Description");
        testNotificationDto.setNotificationStatus(NotificationStatus.UNREAD.toString());
        testNotificationDto.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        testNotificationDto.setSourceComponent("Test Component");
        testNotificationDto.setPriority(Priority.MID.toString());
    }

    // =========================== Delete Notification Tests ===========================

    @DisplayName("Delete Notification By Id : Success")
    @Test
    void deleteNotificationById_Success() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When
        notificationService.deleteNotificationById(TEST_NOTIFICATION_ID, TEST_USER_ID);

        // Then
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository).deleteById(TEST_NOTIFICATION_ID);
    }

    @DisplayName("Delete Notification By Id : Super Admin Can Delete")
    @Test
    void deleteNotificationById_SuperAdminCanDelete() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When
        notificationService.deleteNotificationById(TEST_NOTIFICATION_ID, SUPER_ADMIN_USER_ID);

        // Then
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository).deleteById(TEST_NOTIFICATION_ID);
    }

    @DisplayName("Delete Notification By Id : Notification Not Found")
    @Test
    void deleteNotificationById_NotificationNotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.deleteNotificationById(TEST_NOTIFICATION_ID, TEST_USER_ID)
        );

        assertEquals("Notification with id " + TEST_NOTIFICATION_ID + " not found", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository, never()).deleteById(any());
    }

    @DisplayName("Delete Notification By Id : Forbidden Access")
    @Test
    void deleteNotificationById_ForbiddenAccess_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When & Then
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> notificationService.deleteNotificationById(TEST_NOTIFICATION_ID, OTHER_USER_ID)
        );

        assertEquals("You are not allowed to access this notification", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository, never()).deleteById(any());
    }

    // =========================== Update Notification Status To Read Tests ===========================

    @DisplayName("Update Notification Status To Read : Success")
    @Test
    void updateNotificationStatusToRead_Success() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(testNotification);

        // When
        notificationService.updateNotificationStatusToRead(TEST_NOTIFICATION_ID, TEST_USER_ID);

        // Then
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository).save(testNotification);
        assertEquals(NotificationStatus.READ.toString(), testNotification.getNotificationStatus());
    }

    @DisplayName("Update Notification Status To Read : Super Admin Can Update")
    @Test
    void updateNotificationStatusToRead_SuperAdminCanUpdate() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(testNotification);

        // When
        notificationService.updateNotificationStatusToRead(TEST_NOTIFICATION_ID, SUPER_ADMIN_USER_ID);

        // Then
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository).save(testNotification);
        assertEquals(NotificationStatus.READ.toString(), testNotification.getNotificationStatus());
    }

    @DisplayName("Update Notification Status To Read : Notification Not Found")
    @Test
    void updateNotificationStatusToRead_NotificationNotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.updateNotificationStatusToRead(TEST_NOTIFICATION_ID, TEST_USER_ID)
        );

        assertEquals("Notification with id " + TEST_NOTIFICATION_ID + " not found", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository, never()).save(any());
    }

    @DisplayName("Update Notification Status To Read : Forbidden Access")
    @Test
    void updateNotificationStatusToRead_ForbiddenAccess_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When & Then
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> notificationService.updateNotificationStatusToRead(TEST_NOTIFICATION_ID, OTHER_USER_ID)
        );

        assertEquals("You are not allowed to access this notification", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository, never()).save(any());
    }

    // =========================== Retrieve All Notifications Per User Id Tests ===========================

    @DisplayName("Retrieve All Notifications Per User Id : Success")
    @Test
    void retrieveAllNotificationsPerUserId_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Collections.singletonList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByUserId(TEST_USER_ID, pageable))
                .thenReturn(notificationPage);
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotificationsPerUserId(TEST_USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testNotificationDto, result.getContent().getFirst());
        verify(notificationRepository).findByUserId(TEST_USER_ID, pageable);
        verify(modelMapper).map(testNotification, NotificationDto.class);
    }

    @DisplayName("Retrieve All Notifications Per User Id : Empty Page")
    @Test
    void retrieveAllNotificationsPerUserId_EmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(notificationRepository.findByUserId(TEST_USER_ID, pageable))
                .thenReturn(emptyPage);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotificationsPerUserId(TEST_USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(notificationRepository).findByUserId(TEST_USER_ID, pageable);
        verify(modelMapper, never()).map(any(), any());
    }

    @DisplayName("Retrieve All Notifications Per User Id : Multiple Notifications")
    @Test
    void retrieveAllNotificationsPerUserId_MultipleNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(TEST_USER_ID);
        notification2.setNotificationStatus(NotificationStatus.READ.toString());

        NotificationDto notificationDto2 = new NotificationDto();
        notificationDto2.setId(2L);
        notificationDto2.setUserId(TEST_USER_ID);
        notificationDto2.setNotificationStatus(NotificationStatus.READ.toString());

        List<Notification> notifications = Arrays.asList(testNotification, notification2);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 2);

        when(notificationRepository.findByUserId(TEST_USER_ID, pageable))
                .thenReturn(notificationPage);
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);
        when(modelMapper.map(notification2, NotificationDto.class))
                .thenReturn(notificationDto2);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotificationsPerUserId(TEST_USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(testNotificationDto, result.getContent().get(0));
        assertEquals(notificationDto2, result.getContent().get(1));
    }

    // =========================== Retrieve Unread Notifications Per User Id Tests ===========================

    @DisplayName("Retrieve Unread Notifications Per User Id : Success")
    @Test
    void retrieveUnreadNotificationsPerUserId_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Collections.singletonList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByUserIdAndNotificationStatus(
                TEST_USER_ID, NotificationStatus.UNREAD.toString(), pageable))
                .thenReturn(notificationPage);
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);

        // When
        Page<NotificationDto> result = notificationService.retrieveUnreadNotificationsPerUserId(TEST_USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testNotificationDto, result.getContent().getFirst());
        verify(notificationRepository).findByUserIdAndNotificationStatus(
                TEST_USER_ID, NotificationStatus.UNREAD.toString(), pageable);
        verify(modelMapper).map(testNotification, NotificationDto.class);
    }

    @DisplayName("Retrieve Unread Notifications Per User Id : Empty Page")
    @Test
    void retrieveUnreadNotificationsPerUserId_EmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(notificationRepository.findByUserIdAndNotificationStatus(
                TEST_USER_ID, NotificationStatus.UNREAD.toString(), pageable))
                .thenReturn(emptyPage);

        // When
        Page<NotificationDto> result = notificationService.retrieveUnreadNotificationsPerUserId(TEST_USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(notificationRepository).findByUserIdAndNotificationStatus(
                TEST_USER_ID, NotificationStatus.UNREAD.toString(), pageable);
        verify(modelMapper, never()).map(any(), any());
    }

    // =========================== Retrieve Notification By ID Tests ===========================

    @DisplayName("Retrieve Notification By ID : Success")
    @Test
    void retrieveNotificationById_Success() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);

        // When
        NotificationDto result = notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(testNotificationDto, result);
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(modelMapper).map(testNotification, NotificationDto.class);
    }

    @DisplayName("Retrieve Notification By ID : Super Admin Can Access")
    @Test
    void retrieveNotificationById_SuperAdminCanAccess() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);

        // When
        NotificationDto result = notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, SUPER_ADMIN_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(testNotificationDto, result);
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(modelMapper).map(testNotification, NotificationDto.class);
    }

    @DisplayName("Retrieve Notification By ID : Notification Not Found")
    @Test
    void retrieveNotificationById_NotificationNotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, TEST_USER_ID)
        );

        assertEquals("Notification with id " + TEST_NOTIFICATION_ID + " not found", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(modelMapper, never()).map(any(), any());
    }

    @DisplayName("Retrieve Notification By ID : Forbidden Access")
    @Test
    void retrieveNotificationById_ForbiddenAccess_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When & Then
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, OTHER_USER_ID)
        );

        assertEquals("You are not allowed to access this notification", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(modelMapper, never()).map(any(), any());
    }

    @DisplayName("Retrieve Notification By ID : Mapping Exception")
    @Test
    void retrieveNotificationById_MappingException_ThrowsModelMappingException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenThrow(new MappingException(List.of(new ErrorMessage("Mapping error"))));

        // When & Then
        ModelMappingException exception = assertThrows(
                ModelMappingException.class,
                () -> notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, TEST_USER_ID)
        );

        assertEquals("Unable to convert Notification model to DTO", exception.getMessage());
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(modelMapper).map(testNotification, NotificationDto.class);
    }

    // =========================== Helper Method Tests ===========================

    @DisplayName("Convert Page Of Notifications To List Of Dto : Success")
    @Test
    void convertPageOfNotificationsToListOfDto_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Collections.singletonList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);

        // When
        List<NotificationDto> result = notificationService.convertPageOfNotificationsToListOfDto(notificationPage);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotificationDto, result.getFirst());
        verify(modelMapper).map(testNotification, NotificationDto.class);
    }

    @DisplayName("Convert Page Of Notifications To List Of Dto : Empty Page")
    @Test
    void convertPageOfNotificationsToListOfDto_EmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        // When
        List<NotificationDto> result = notificationService.convertPageOfNotificationsToListOfDto(emptyPage);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(modelMapper, never()).map(any(), any());
    }

    // =========================== Create Notifications For Each User Tests ===========================
    @DisplayName("Create Notifications For Each User : Success")
    @Test
    void createNotificationsForEachUser_Success() {
        // Given
        List<UserDto> users = createListOfUsers();
        NotificationDto notificationDto = testNotificationDto;

        Notification mappedNotification1 = new Notification();
        Notification mappedNotification2 = new Notification();
        Notification mappedNotification3 = new Notification();

        when(modelMapper.map(notificationDto, Notification.class))
                .thenReturn(mappedNotification1, mappedNotification2, mappedNotification3);
        when(notificationRepository.saveAll(any()))
                .thenReturn(Collections.emptyList());

        // When
        notificationService.createNotificationsForEachUser(users, notificationDto);

        // Then
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        verify(modelMapper, times(3)).map(notificationDto, Notification.class);

        List<Notification> savedNotifications = captor.getValue();
        assertEquals(3, savedNotifications.size());

        // Verify user notifications
        assertEquals("user0", savedNotifications.getFirst().getUserId());
        assertEquals("Test User0", savedNotifications.getFirst().getUser());
        assertEquals("user1", savedNotifications.get(1).getUserId());
        assertEquals("Test User1", savedNotifications.get(1).getUser());

        // Verify SUPER_ADMIN notification
        assertEquals(SUPER_ADMIN_USER_ID, savedNotifications.get(2).getUserId());
        assertEquals("SUPER_ADMIN", savedNotifications.get(2).getUser());
    }

    @DisplayName("Create Notifications For Each User : Empty User List")
    @Test
    void createNotificationsForEachUser_EmptyUserList_CreatesOnlySuperAdminNotification() {
        // Given
        List<UserDto> users = Collections.emptyList();
        NotificationDto notificationDto = testNotificationDto;

        Notification mappedNotification = new Notification();
        when(modelMapper.map(notificationDto, Notification.class))
                .thenReturn(mappedNotification);
        when(notificationRepository.saveAll(any()))
                .thenReturn(Collections.emptyList());

        // When
        notificationService.createNotificationsForEachUser(users, notificationDto);

        // Then
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        verify(modelMapper, times(1)).map(notificationDto, Notification.class);

        List<Notification> savedNotifications = captor.getValue();
        assertEquals(1, savedNotifications.size());
        assertEquals(SUPER_ADMIN_USER_ID, savedNotifications.getFirst().getUserId());
        assertEquals("SUPER_ADMIN", savedNotifications.getFirst().getUser());
    }

    @DisplayName("Create Notifications For Each User : Mapping Exception")
    @Test
    void createNotificationsForEachUser_MappingException_LogsError() {
        // Given
        List<UserDto> users = createListOfUsers();
        NotificationDto notificationDto = testNotificationDto;

        when(modelMapper.map(notificationDto, Notification.class))
                .thenThrow(new MappingException(List.of(new ErrorMessage("Mapping failed"))));

        // When
        notificationService.createNotificationsForEachUser(users, notificationDto);

        // Then
        verify(modelMapper).map(notificationDto, Notification.class);
        verify(notificationRepository, never()).saveAll(any());
    }

    // =========================== Retry Mechanism Tests ===========================
    @DisplayName("Retrieve User Ids Per Organization : Should retry on 404 error")
    @Disabled
    @Test
    void retrieveUserIdsPerOrganization_ShouldRetryOn404Error() {
        // Given
        String organization = "test-org";
        String token = "test-token";
        NotificationService spyService = spy(notificationService);

        ReflectionTestUtils.setField(spyService, "userManagerUrl", "http://localhost:8094");
        when(spyService.retrieveComponentJwtToken()).thenReturn(token);

        HttpClientErrorException notFoundException =
            new HttpClientErrorException(HttpStatus.NOT_FOUND, "Organization not found");

        when(restTemplate.exchange(any(), any(), any(), eq(UserManagerResponse.class)))
            .thenThrow(notFoundException);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> spyService.retrieveUserIdsPerOrganization(organization)
        );

        assertEquals("Organization not found yet in Keycloak..", exception.getMessage());
    }

    @DisplayName("Retrieve User Ids Per User Roles And Organization : Should retry on 404 error")
    @Disabled
    @Test
    void retrieveUserIdsPerUserRolesAndOrganization_ShouldRetryOn404Error() {
        // Given
        Set<String> userRoles = Set.of("USER_ROLE");
        String organization = "test-org";
        String token = "test-token";
        NotificationService spyService = spy(notificationService);

        ReflectionTestUtils.setField(spyService, "userManagerUrl", "http://localhost:8094");
        when(spyService.retrieveComponentJwtToken()).thenReturn(token);

        HttpClientErrorException notFoundException =
            new HttpClientErrorException(HttpStatus.NOT_FOUND, "Role not found");

        when(restTemplate.exchange(any(), any(), any(), eq(UserManagerResponse.class)))
            .thenThrow(notFoundException);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> spyService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, organization)
        );

        assertEquals("Organization or Role not found yet in Keycloak..", exception.getMessage());
    }

    // =========================== Retrieve User Ids Per Organization Tests ===========================
    @Disabled
    @DisplayName("Retrieve User Ids Per Organization : Success")
    @Test
    void retrieveUserIdsPerOrganization_Success() {
        // Given
        String organization = "test-org";
        String token = "test-token";
        UserManagerResponse userManagerResponse = new UserManagerResponse();
        List<UserDto> expectedUsers = createListOfUsers();
        userManagerResponse.setData(expectedUsers);

        // Spy the notification service
        NotificationService spyService = spy(notificationService);
        ReflectionTestUtils.setField(spyService, "userManagerUrl", "http://localhost:8094");
        ReflectionTestUtils.setField(spyService, "tokenUri", "http://token-url:9080");
        when(spyService.retrieveComponentJwtToken()).thenReturn(token);

        when(restTemplate.exchange(
                eq("http://localhost:8094/api/users/pilots/" + organization),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserManagerResponse.class)
        )).thenReturn(ResponseEntity.ok(userManagerResponse));

        // When
        List<UserDto> result = spyService.retrieveUserIdsPerOrganization(organization);

        // Then
        assertEquals(expectedUsers, result);
        verify(restTemplate).exchange(
                eq("http://localhost:8094/api/users/pilots/" + organization),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserManagerResponse.class)
        );
    }

    @Disabled
    @DisplayName("Retrieve User Ids Per Organization : Null Token")
    @Test
    void retrieveUserIdsPerOrganization_NullToken_ReturnsEmptyList() {
        // Given
        String organization = "test-org";
        when(notificationService.retrieveComponentJwtToken()).thenReturn(null);

        // When
        List<UserDto> result = notificationService.retrieveUserIdsPerOrganization(organization);

        // Then
        assertEquals(Collections.emptyList(), result);
        verify(restTemplate, never()).exchange(any(), any(), any(), any(Class.class));
    }

    @Disabled
    @DisplayName("Retrieve User Ids Per Organization : Rest Client Exception")
    @Test
    void retrieveUserIdsPerOrganization_RestClientException_ReturnsEmptyList() {
        // Given
        String organization = "test-org";
        String token = "test-token";
        when(notificationService.retrieveComponentJwtToken()).thenReturn(token);
        when(restTemplate.exchange(any(), any(), any(), any(Class.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // When
        List<UserDto> result = notificationService.retrieveUserIdsPerOrganization(organization);

        // Then
        assertEquals(Collections.emptyList(), result);
    }

    @Disabled
    @DisplayName("Retrieve User Ids Per Organization : Non-2xx Response")
    @Test
    void retrieveUserIdsPerOrganization_Non2xxResponse_ReturnsEmptyList() {
        // Given
        String organization = "test-org";
        String token = "test-token";
        when(notificationService.retrieveComponentJwtToken()).thenReturn(token);
        when(restTemplate.exchange(any(), any(), any(), any(Class.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        // When
        List<UserDto> result = notificationService.retrieveUserIdsPerOrganization(organization);

        // Then
        assertEquals(Collections.emptyList(), result);
    }

    // =========================== Retrieve User Ids Per User Roles And Organization Tests ===========================
    @Disabled
    @DisplayName("Retrieve User Ids Per User Roles And Organization : Success")
    @Test
    void retrieveUserIdsPerUserRolesAndOrganization_Success() {
        // Given
        Set<String> userRoles = Set.of("USER_ROLE1", "USER_ROLE2");
        String organization = "test-org";
        String token = "test-token";
        String userManagerUrl = "http://user-manager:8093";

        UserManagerResponse adminResponse = new UserManagerResponse();
        adminResponse.setData(List.of(createMockUser(1)));

        UserManagerResponse userResponse = new UserManagerResponse();
        userResponse.setData(List.of(createMockUser(2)));

        when(notificationService.retrieveComponentJwtToken()).thenReturn(token);
        when(restTemplate.exchange(
                eq(userManagerUrl + "/api/users/pilots/" + organization + "/roles/ADMIN"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserManagerResponse.class)
        )).thenReturn(ResponseEntity.ok(adminResponse));

        when(restTemplate.exchange(
                eq(userManagerUrl + "/api/users/pilots/" + organization + "/roles/USER"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserManagerResponse.class)
        )).thenReturn(ResponseEntity.ok(userResponse));

        // When
        List<UserDto> result = notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, organization);

        // Then
        assertEquals(2, result.size());
        verify(restTemplate, times(2)).exchange(any(), eq(HttpMethod.GET), any(), eq(UserManagerResponse.class));
    }

    @Disabled
    @DisplayName("Retrieve User Ids Per User Roles And Organization : Null Token")
    @Test
    void retrieveUserIdsPerUserRolesAndOrganization_NullToken_ReturnsEmptyList() {
        // Given
        Set<String> userRoles = Set.of("TEST_ROLE");
        String organization = "test-org";
        when(notificationService.retrieveComponentJwtToken()).thenReturn(null);

        // When
        List<UserDto> result = notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, organization);

        // Then
        assertEquals(Collections.emptyList(), result);
    }

    @Disabled
    @DisplayName("Retrieve User Ids Per User Roles And Organization : Rest Client Exception")
    @Test
    void retrieveUserIdsPerUserRolesAndOrganization_RestClientException_ContinuesWithOtherRoles() {
        // Given
        Set<String> userRoles = Set.of("USER_ROLE1", "USER_ROLE2");
        String organization = "test-org";
        String token = "test-token";

        UserManagerResponse userResponse = new UserManagerResponse();
        userResponse.setData(List.of(createMockUser(1)));

        NotificationService spyService = spy(notificationService);
        when(spyService.retrieveComponentJwtToken()).thenReturn(token);

        when(restTemplate.exchange(
                contains("/roles/USER_ROLE1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserManagerResponse.class)
        )).thenThrow(new RestClientException("Connection failed"));

        when(restTemplate.exchange(
                contains("/roles/USER_ROLE2"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserManagerResponse.class)
        )).thenReturn(ResponseEntity.ok(userResponse));

        // When
        List<UserDto> result = spyService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, organization);

        // Then
        assertEquals(1, result.size());
        assertEquals("user1", result.getFirst().userId());
    }

    // =========================== Edge Case Tests ===========================

    @DisplayName("Validate And Get Notification : Success")
    @Test
    void validateAndGetNotification_NullUserId_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When & Then
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, null)
        );

        assertEquals("You are not allowed to access this notification", exception.getMessage());
    }

    @DisplayName("Validate And Get Notification : Empty User Id")
    @Test
    void validateAndGetNotification_EmptyUserId_ThrowsException() {
        // Given
        when(notificationRepository.findById(TEST_NOTIFICATION_ID))
                .thenReturn(Optional.of(testNotification));

        // When & Then
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> notificationService.retrieveNotificationById(TEST_NOTIFICATION_ID, "")
        );

        assertEquals("You are not allowed to access this notification", exception.getMessage());
    }

    //=============================== JWT Token Retrieval Test =============================
    @DisplayName("Retrieve Component JWT Token: Failure")
    @Test
    void givenMockToken_whenRetrieveComponentJwtTokenFails_thenReturnNull() {
        // Given
        ReflectionTestUtils.setField(notificationService, "tokenUri", "http://localhost:8080/token");
        lenient().when(restTemplate.exchange(eq("http://localhost:8080/token"), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenThrow(RestClientException.class);

        // When
        String result = notificationService.retrieveComponentJwtToken();

        // Then
        assertNull(result);
    }

    // =========================== Pagination Tests ===========================

    @DisplayName("Retrieve All Notifications Per User Id : Success")
    @Test
    void retrieveAllNotificationsPerUserId_WithPagination() {
        // Given
        Pageable pageable = PageRequest.of(1, 5);
        List<Notification> notifications = Collections.singletonList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 10);

        when(notificationRepository.findByUserId(TEST_USER_ID, pageable))
                .thenReturn(notificationPage);
        when(modelMapper.map(testNotification, NotificationDto.class))
                .thenReturn(testNotificationDto);

        // When
        Page<NotificationDto> result = notificationService.retrieveAllNotificationsPerUserId(TEST_USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(10, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
    }

    // =========================== Helper Methods ===========================
    private List<UserDto> createListOfUsers() {
        List<UserDto> users = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            UserDto user = createMockUser(i);
            users.add(user);
        }
        return users;
    }

    private UserDto createMockUser(int index) {
        return new UserDto("user" + index,
                "username" + index,
                "Test",
                "User" + index,
                "email" + index,
                "PILOT_ROLE_" + index,
                "PILOT_CODE_" + index,
                "USER_ROLE_" + index);
    }
}
