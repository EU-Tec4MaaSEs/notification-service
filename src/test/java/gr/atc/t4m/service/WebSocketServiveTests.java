package gr.atc.t4m.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    private static final String TEST_MESSAGE = "Test notification message";
    private static final String TEST_TOPIC_NAME = "admin-notifications";
    private static final String TEST_USER_ID = "user123";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(messagingTemplate);
    }

    @DisplayName("Notify Users and Roles via WebSocket : Success with Correct Topic Path")
    @Test
    void givenValidParameters_whenNotifyUsersAndRolesViaWebSocket_thenSuccess() throws Exception {
        // Given
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(messagingTemplate).convertAndSend(topicCaptor.capture(), messageCaptor.capture());

        // When
        webSocketService.notifyUsersAndRolesViaWebSocket(TEST_MESSAGE, TEST_TOPIC_NAME);
        Thread.sleep(100); // Wait for async

        // Then
        String capturedTopic = topicCaptor.getValue();
        String capturedMessage = messageCaptor.getValue();

        assertThat(capturedTopic).isEqualTo("/topic/notifications/" + TEST_TOPIC_NAME);
        assertThat(capturedMessage).isEqualTo(TEST_MESSAGE);
    }

    @DisplayName("Notify Users and Roles via WebSocket : Success / No Exception Thrown")
    @Test
    void givenMessagingException_whenNotifyUsersAndRolesViaWebSocket_thenSuccess() throws Exception {
        // Given
        doThrow(new MessagingException("WebSocket connection failed"))
                .when(messagingTemplate).convertAndSend(anyString(), anyString());

        // When & Then
        assertDoesNotThrow(() -> {
            webSocketService.notifyUsersAndRolesViaWebSocket(TEST_MESSAGE, TEST_TOPIC_NAME);
            Thread.sleep(100); // Wait for async
        });

        verify(messagingTemplate, times(1)).convertAndSend(anyString(), anyString());
    }

    @DisplayName("Notify User via WebSocket : Success with Correct Parameters")
    @Test
    void givenValidParameters_whenNotifyUserViaWebSocket_thenSuccess() throws Exception {
        // Given
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), anyString());

        // When
        webSocketService.notifyUserViaWebSocket(TEST_USER_ID, TEST_MESSAGE);
        Thread.sleep(100);

        // Then
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(TEST_USER_ID, "/queue/notifications", TEST_MESSAGE);
    }

    @DisplayName("Notify User via WebSocket : Success / No Exception Thrown")
    @Test
    void givenMessagingException_whenNotifyUserViaWebSocket_thenSuccess() throws Exception {
        // Given
        doThrow(new MessagingException("User not connected"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), anyString());

        // When & Then
        assertDoesNotThrow(() -> {
            webSocketService.notifyUserViaWebSocket(TEST_USER_ID, TEST_MESSAGE);
            Thread.sleep(100);
        });

        // Verify the method was called despite the exception
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq(TEST_USER_ID), eq("/queue/notifications"), eq(TEST_MESSAGE));
    }

    @DisplayName("Notify Users and Roles via WebSocket : Success with Empty Message")
    @Test
    void givenEmptyMessage_whenNotifyUsersAndRolesViaWebSocket_thenSuccess() throws Exception {
        // Given
        String emptyMessage = "";
        doNothing().when(messagingTemplate).convertAndSend(anyString(), anyString());

        // When
        webSocketService.notifyUsersAndRolesViaWebSocket(emptyMessage, TEST_TOPIC_NAME);
        Thread.sleep(100);

        // Then
        String expectedTopic = "/topic/notifications/" + TEST_TOPIC_NAME;
        verify(messagingTemplate, times(1)).convertAndSend(expectedTopic, emptyMessage);
    }

    @DisplayName("Notify User via WebSocket : Success with Empty Message")
    @Test
    void givenEmptyMessage_whenNotifyUserViaWebSocket_thenSuccess() throws Exception {
        // Given
        String emptyMessage = "";
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), anyString());

        // When
        webSocketService.notifyUserViaWebSocket(TEST_USER_ID, emptyMessage);
        Thread.sleep(100);

        // Then
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(TEST_USER_ID, "/queue/notifications", emptyMessage);
    }
}