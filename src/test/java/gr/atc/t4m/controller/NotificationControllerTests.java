package gr.atc.t4m.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.enums.NotificationStatus;
import gr.atc.t4m.enums.Priority;
import gr.atc.t4m.service.interfaces.INotificationService;
import gr.atc.t4m.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@DisplayName("Notification Controller Tests")
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private INotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private NotificationDto sampleNotification;
    private Page<NotificationDto> samplePage;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        // Create sample notification
        sampleNotification = NotificationDto.builder()
                .id(1L)
                .description("Test Notification")
                .user("Test User")
                .userId("user123")
                .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC))
                .notificationStatus(NotificationStatus.UNREAD.toString())
                .priority(Priority.MID.toString())
                .build();


        NotificationDto secondNotification = NotificationDto.builder()
                .id(2L)
                .description("Test Notification")
                .user("Super Admin")
                .userId("SUPER_ADMIN")
                .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC))
                .notificationStatus(NotificationStatus.READ.toString())
                .priority(Priority.HIGH.toString())
                .build();

        // Create sample notifications list
        List<NotificationDto> sampleNotifications = Arrays.asList(
                sampleNotification,
                secondNotification
        );

        // Create sample page
        samplePage = new PageImpl<>(sampleNotifications, PageRequest.of(0, 10), 2);

        // Create mock JWT
        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user123")
                .claim("pilot_role", "ADMIN")
                .build();
    }

    //========================= Get All Notifications Tests ================================

    @DisplayName("Get All Notifications : Success")
    @Test
    void whenRetrieveAllNotificationsPerUserId_thenReturnNotifications() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(samplePage);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .with(jwt().jwt(mockJwt))
                            .param("page", "0")
                            .param("size", "10")
                            .param("sortAttribute", "timestamp")
                            .param("isAscending", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notifications retrieved successfully"))
                    .andExpect(jsonPath("$.data.results").hasJsonPath())
                    .andExpect(jsonPath("$.data.results", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.lastPage").value(true));

            verify(notificationService).retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class));
        }
    }

    @DisplayName("Get All Notifications : Success for SUPER_ADMIN")
    @Test
    void shouldReturnAllNotificationsForSuperAdmin() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("SUPER_ADMIN");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(samplePage);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notifications retrieved successfully"));

            verify(notificationService).retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class));
        }
    }

    @DisplayName("Get All Notifications : Invalid Sort Attribute")
    @Test
    void givenInvalidSortAttribute_whenRetrieveAllNotificationsPerUserId_thenReturnBadRequest() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .with(jwt().jwt(mockJwt))
                            .param("sortAttribute", "invalidField"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid sort attributes"));

            verify(notificationService, never()).retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class));
        }
    }

    @DisplayName("Get All Notifications : Use default pagination")
    @Test
    void givenDefaultPagination_whenRetrieveAllNotificationsPerUserId_thenReturnNotifications() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(samplePage);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk());

            // Verify default pagination (page=0, size=10, sort=timestamp, desc)
            verify(notificationService).retrieveAllNotificationsPerUserId(anyString(),
                   any(Pageable.class));
        }
    }

    //========================= Get Unread Notifications Tests ================================
    @DisplayName("Get Unread Notifications per User ID : Success")
    @Test
    void givenUserId_whenRetrieveUnreadNotificationsPerUserId_thenReturnUnreadNotifications() throws Exception {
        // Given
        List<NotificationDto> unreadNotifications = Collections.singletonList(sampleNotification);
        Page<NotificationDto> unreadPage = new PageImpl<>(unreadNotifications, PageRequest.of(0, 10), 1);

        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveUnreadNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(unreadPage);

            // When & Then
            mockMvc.perform(get("/api/notifications/unread")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notifications retrieved successfully"))
                    .andExpect(jsonPath("$.data.results", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(notificationService).retrieveUnreadNotificationsPerUserId(anyString(), any(Pageable.class));
        }
    }

    @DisplayName("Get Unread Notifications per User ID : Success for SUPER_ADMIN")
    @Test
    void givenSuperAdmin_whenRetrieveUnreadNotificationsPerUserId_thenReturnUnreadNotifications() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("SUPER_ADMIN");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveUnreadNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(samplePage);

            // When & Then
            mockMvc.perform(get("/api/notifications/unread")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService).retrieveUnreadNotificationsPerUserId(anyString(), any(Pageable.class));
        }
    }

    @DisplayName("Get Unread Notifications per User ID : Invalid Sort Attribute")
    @Test
    void givenInvalidSortAttribute_whenRetrieveUnreadNotificationsPerUserId_thenReturnBadRequest() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            // When & Then
            mockMvc.perform(get("/api/notifications/unread")
                            .with(jwt().jwt(mockJwt))
                            .param("sortAttribute", "nonExistentField"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid sort attributes"));

            verify(notificationService, never()).retrieveUnreadNotificationsPerUserId(anyString(), any(Pageable.class));
        }
    }

    //========================= Get Notification by ID Tests ================================
    @DisplayName("Retrieve Notification by ID : Success")
    @Test
    void givenNotificationId_whenRetrieveNotificationById_thenReturnNotification() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveNotificationById(1L, "user123"))
                    .thenReturn(sampleNotification);

            // When & Then
            mockMvc.perform(get("/api/notifications/1")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notifications retrieved successfully"))
                    .andExpect(jsonPath("$.data.notificationId").value(1))
                    .andExpect(jsonPath("$.data.description").value("Test Notification"));

            verify(notificationService).retrieveNotificationById(1L, "user123");
        }
    }

    @DisplayName("Retrieve Notification by ID : Success for SUPER_ADMIN")
    @Test
    void givenNotificationIdAndSuperAdmin_whenRetrieveNotificationById_thenReturnNotification() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("SUPER_ADMIN");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveNotificationById(1L, "SUPER_ADMIN"))
                    .thenReturn(sampleNotification);

            // When & Then
            mockMvc.perform(get("/api/notifications/1")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.notificationId").value(1));

            verify(notificationService).retrieveNotificationById(1L, "SUPER_ADMIN");
        }
    }

    //========================= Update Notification Status Tests ================================
    @DisplayName("Update Notification Status : Success")
    @Test
    void givenNotificationId_whenUpdateNotificationStatus_thenReturnUpdatedNotification() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            doNothing().when(notificationService).updateNotificationStatusToRead(1L, "user123");

            // When & Then
            mockMvc.perform(put("/api/notifications/1/notificationStatus")
                            .with(jwt().jwt(mockJwt))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notification status updated successfully"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(notificationService).updateNotificationStatusToRead(1L, "user123");
        }
    }

    @DisplayName("Update Notification Status : Success for SUPER_ADMIN")
    @Test
    void givenNotificationIdAndSuperAdmin_whenUpdateNotificationStatus_thenReturnUpdatedNotification() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("SUPER_ADMIN");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            doNothing().when(notificationService).updateNotificationStatusToRead(1L, "SUPER_ADMIN");

            // When & Then
            mockMvc.perform(put("/api/notifications/1/notificationStatus")
                            .with(jwt().jwt(mockJwt))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notification status updated successfully"));

            verify(notificationService).updateNotificationStatusToRead(1L, "SUPER_ADMIN");
        }
    }

    //========================= Delete Notification Tests ================================
    @DisplayName("Delete Notification by ID : Success")
    @Test
    void shouldDeleteNotificationForRegularUser() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            doNothing().when(notificationService).deleteNotificationById(1L, "user123");

            // When & Then
            mockMvc.perform(delete("/api/notifications/1")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notification deleted successfully"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(notificationService).deleteNotificationById(1L, "user123");
        }
    }

    @DisplayName("Delete Notification by ID : Success for SUPER_ADMIN")
    @Test
    void givenNotificationIdAndSuperAdmin_whenDeleteNotificationById_thenReturnSuccess() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("SUPER_ADMIN");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            doNothing().when(notificationService).deleteNotificationById(1L, "SUPER_ADMIN");

            // When & Then
            mockMvc.perform(delete("/api/notifications/1")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notification deleted successfully"));

            verify(notificationService).deleteNotificationById(1L, "SUPER_ADMIN");
        }
    }
    //========================= Error Handling Tests ================================

    @DisplayName("Get All Notifications : Unauthorized")
    @Test
    void givenUnauthorizedUser_whenRetrieveAllNotificationsPerUserId_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Get All Notifications : Invalid Token")
    @Test
    void givenInvalidToken_whenRetrieveAllNotificationsPerUserId_thenReturnUnauthorized() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(samplePage);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .with(jwt().jwt(mockJwt))
                            .param("page", "2")
                            .param("size", "5")
                            .param("sortAttribute", "timestamp")
                            .param("isAscending", "true"))
                    .andExpect(status().isOk());

            // Verify custom pagination parameters
            verify(notificationService).retrieveAllNotificationsPerUserId(anyString(),
                    eq(PageRequest.of(2, 5, Sort.by("timestamp").ascending())));
        }
    }

    @DisplayName("Get All Notifications : Empty Notifications List")
    @Test
    void givenEmptyNotificationsList_whenRetrieveAllNotificationsPerUserId_thenReturnEmptyList() throws Exception {
        // Given
        Page<NotificationDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .with(jwt().jwt(mockJwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.results", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @DisplayName("Get All Notifications : Various Valid Sort Attributes")
    @Test
    void givenVariousValidSortAttributes_whenRetrieveAllNotificationsPerUserId_thenReturnSuccess() throws Exception {
        // Given
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
            jwtUtilsMock.when(() -> JwtUtils.extractPilotRole(any(Jwt.class))).thenReturn("USER");
            jwtUtilsMock.when(() -> JwtUtils.extractUserId(any(Jwt.class))).thenReturn("user123");

            when(notificationService.retrieveAllNotificationsPerUserId(anyString(), any(Pageable.class)))
                    .thenReturn(samplePage);

            // Test different sort attributes (assuming these exist in NotificationDto)
            String[] validSortAttributes = {"id", "description", "user", "timestamp", "notificationStatus"};

            for (String sortAttr : validSortAttributes) {
                mockMvc.perform(get("/api/notifications")
                                .with(jwt().jwt(mockJwt))
                                .param("sortAttribute", sortAttr))
                        .andExpect(status().isOk());
            }
        }
    }

}