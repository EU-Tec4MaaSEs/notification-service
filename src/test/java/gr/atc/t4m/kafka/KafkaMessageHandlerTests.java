package gr.atc.t4m.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.config.properties.KafkaProperties;
import gr.atc.t4m.dto.EventDto;
import gr.atc.t4m.dto.EventMappingDto;
import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.dto.UserDto;
import gr.atc.t4m.enums.NotificationStatus;
import gr.atc.t4m.enums.Priority;
import gr.atc.t4m.exception.CustomExceptions.ModelMappingException;
import gr.atc.t4m.exception.CustomExceptions.ResourceNotFoundException;
import gr.atc.t4m.service.interfaces.IEventMappingService;
import gr.atc.t4m.service.interfaces.INotificationService;
import gr.atc.t4m.service.interfaces.IWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {"test-topic", "event-topic", "notification-topic"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=test-group",
        "ctest-topic,event-topic,notification-topic"
})
public class KafkaMessageHandlerTests {

    @Mock
    private INotificationService notificationService;

    @Mock
    private IEventMappingService eventMappingService;

    @Mock
    private IWebSocketService webSocketService;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaProperties.Consumer consumer;

    private ObjectMapper objectMapper;
    private KafkaMessageHandler kafkaMessageHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Mock KafkaProperties
        when(kafkaProperties.consumer()).thenReturn(consumer);
        when(consumer.getTopicsList()).thenReturn(Arrays.asList("test-topic", "event-topic"));

        kafkaMessageHandler = new KafkaMessageHandler(
                notificationService,
                eventMappingService,
                objectMapper,
                kafkaProperties,
                webSocketService
        );
    }

    @Test
    @DisplayName("Should successfully process valid event with existing mappings")
    void shouldProcessValidEventWithExistingMappings() {
        // Given
        EventDto validEvent = createValidEvent();
        String topic = "test-topic";
        Set<String> userRoles = Set.of("ADMIN", "USER");
        List<UserDto> users = createTestUsers();
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(userRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, "TEST_ORG"))
                .thenReturn(users);

        // When
        kafkaMessageHandler.consume(validEvent, topic);

        // Then
        verify(eventMappingService).retrieveEventMappingByTopic(topic);
        verify(notificationService).retrieveUserIdsPerUserRolesAndOrganization(userRoles, "TEST_ORG");

        ArgumentCaptor<List<UserDto>> usersCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NotificationDto> notificationCaptor = ArgumentCaptor.forClass(NotificationDto.class);

        verify(notificationService).createNotificationsForEachUser(usersCaptor.capture(), notificationCaptor.capture());

        assertThat(usersCaptor.getValue()).hasSize(2);
        NotificationDto capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getNotificationStatus()).isEqualTo(NotificationStatus.UNREAD.toString());
        assertThat(capturedNotification.getSourceComponent()).isEqualTo("TestComponent");
        assertThat(capturedNotification.getPriority()).isEqualTo(Priority.HIGH.toString());
        assertThat(capturedNotification.getDescription()).isEqualTo("Test description");
        assertThat(capturedNotification.getType()).isEqualTo("TEST_EVENT");
    }

    @Test
    @DisplayName("Should create default mapping when no mappings exist")
    void shouldCreateDefaultMappingWhenNoMappingsExist() {
        // Given
        EventDto validEvent = createValidEvent();
        String topic = "test-topic";
        List<UserDto> users = createTestUsers();

        when(eventMappingService.retrieveEventMappingByTopic(topic))
                .thenThrow(new ResourceNotFoundException("Mapping not found"));
        when(notificationService.retrieveUserIdsPerOrganization("TEST_ORG"))
                .thenReturn(users);

        // When
        kafkaMessageHandler.consume(validEvent, topic);

        // Then
        verify(eventMappingService).retrieveEventMappingByTopic(topic);
        verify(eventMappingService).createDefaultNotificationMappingAsync(topic);
        verify(notificationService).retrieveUserIdsPerOrganization("TEST_ORG");
        verify(notificationService).createNotificationsForEachUser(any(), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Should handle global event mappings (ALL role)")
    void shouldHandleGlobalEventMappings() {
        // Given
        EventDto validEvent = createValidEvent();
        String topic = "test-topic";
        Set<String> userRoles = Set.of("ALL");
        List<UserDto> users = createTestUsers();
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(userRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerOrganization("TEST_ORG"))
                .thenReturn(users);

        // When
        kafkaMessageHandler.consume(validEvent, topic);

        // Then
        verify(eventMappingService).retrieveEventMappingByTopic(topic);
        verify(notificationService).retrieveUserIdsPerOrganization("TEST_ORG");
        verify(notificationService, never()).retrieveUserIdsPerUserRolesAndOrganization(any(), any());
        verify(notificationService).createNotificationsForEachUser(any(), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Should discard invalid event with null priority")
    void shouldDiscardInvalidEventWithNullPriority() {
        // Given
        EventDto invalidEvent = EventDto.builder()
                .priority(null)
                .sourceComponent("TestComponent")
                .organization("TEST_ORG")
                .type("TEST_EVENT")
                .description("Test description")
                .build();
        String topic = "test-topic";

        // When
        kafkaMessageHandler.consume(invalidEvent, topic);

        // Then
        verify(eventMappingService, never()).retrieveEventMappingByTopic(any());
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should discard invalid event with invalid priority enum")
    void shouldDiscardInvalidEventWithInvalidPriorityEnum() {
        // Given
        EventDto invalidEvent = EventDto.builder()
                .priority("INVALID_PRIORITY")
                .sourceComponent("TestComponent")
                .organization("TEST_ORG")
                .type("TEST_EVENT")
                .description("Test description")
                .build();
        String topic = "test-topic";

        // When
        kafkaMessageHandler.consume(invalidEvent, topic);

        // Then
        verify(eventMappingService, never()).retrieveEventMappingByTopic(any());
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should discard invalid event with null source component")
    void shouldDiscardInvalidEventWithNullSourceComponent() {
        // Given
        EventDto invalidEvent = EventDto.builder()
                .priority(Priority.HIGH.toString())
                .sourceComponent(null)
                .organization("TEST_ORG")
                .type("TEST_EVENT")
                .description("Test description")
                .build();
        String topic = "test-topic";

        // When
        kafkaMessageHandler.consume(invalidEvent, topic);

        // Then
        verify(eventMappingService, never()).retrieveEventMappingByTopic(any());
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should discard invalid event with null organization")
    void shouldDiscardInvalidEventWithNullOrganization() {
        // Given
        EventDto invalidEvent = EventDto.builder()
                .priority(Priority.HIGH.toString())
                .sourceComponent("TestComponent")
                .organization(null)
                .type("TEST_EVENT")
                .description("Test description")
                .build();
        String topic = "test-topic";

        // When
        kafkaMessageHandler.consume(invalidEvent, topic);

        // Then
        verify(eventMappingService, never()).retrieveEventMappingByTopic(any());
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should discard invalid event with null type")
    void shouldDiscardInvalidEventWithNullType() {
        // Given
        EventDto invalidEvent = EventDto.builder()
                .priority(Priority.HIGH.toString())
                .sourceComponent("TestComponent")
                .organization("TEST_ORG")
                .type(null)
                .description("Test description")
                .build();
        String topic = "test-topic";

        // When
        kafkaMessageHandler.consume(invalidEvent, topic);

        // Then
        verify(eventMappingService, never()).retrieveEventMappingByTopic(any());
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should handle ModelMappingException gracefully")
    void shouldHandleModelMappingExceptionGracefully() {
        // Given
        EventDto validEvent = createValidEvent();
        String topic = "test-topic";
        Set<String> userRoles = Set.of("ADMIN");
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(userRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, "TEST_ORG"))
                .thenThrow(new ModelMappingException("Mapping error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> kafkaMessageHandler.consume(validEvent, topic));

        verify(eventMappingService).retrieveEventMappingByTopic(topic);
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should handle generic exceptions gracefully")
    void shouldHandleGenericExceptionsGracefully() {
        // Given
        EventDto validEvent = createValidEvent();
        String topic = "test-topic";
        Set<String> userRoles = Set.of("ADMIN");
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(userRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, "TEST_ORG"))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> kafkaMessageHandler.consume(validEvent, topic));

        verify(eventMappingService).retrieveEventMappingByTopic(topic);
        verify(notificationService, never()).createNotificationsForEachUser(any(), any());
    }

    @Test
    @DisplayName("Should trim and uppercase organization name")
    void shouldTrimAndUppercaseOrganizationName() {
        // Given
        EventDto eventWithUntrimmedOrg = EventDto.builder()
                .priority(Priority.HIGH.toString())
                .sourceComponent("TestComponent")
                .organization("  test_org  ")
                .type("TEST_EVENT")
                .description("Test description")
                .build();
        String topic = "test-topic";
        Set<String> userRoles = Set.of("ADMIN");
        List<UserDto> users = createTestUsers();
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(userRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, "TEST_ORG"))
                .thenReturn(users);

        // When
        kafkaMessageHandler.consume(eventWithUntrimmedOrg, topic);

        // Then
        verify(notificationService).retrieveUserIdsPerUserRolesAndOrganization(userRoles, "TEST_ORG");
    }

    @Test
    @DisplayName("Should handle empty user roles set")
    void shouldHandleEmptyUserRolesSet() {
        // Given
        EventDto validEvent = createValidEvent();
        String topic = "test-topic";
        Set<String> emptyUserRoles = new HashSet<>();
        List<UserDto> users = createTestUsers();
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(emptyUserRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerOrganization("TEST_ORG"))
                .thenReturn(users);

        // When
        kafkaMessageHandler.consume(validEvent, topic);

        // Then
        verify(eventMappingService).retrieveEventMappingByTopic(topic);
        verify(eventMappingService).createDefaultNotificationMappingAsync(topic);
        verify(notificationService).retrieveUserIdsPerOrganization("TEST_ORG");
        verify(notificationService).createNotificationsForEachUser(any(List.class), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Should handle notification generation with all fields")
    void shouldHandleNotificationGenerationWithAllFields() {
        // Given
        EventDto validEvent = EventDto.builder()
                .priority(Priority.MID.toString())
                .sourceComponent("AnotherComponent")
                .organization("ANOTHER_ORG")
                .type("ANOTHER_EVENT")
                .description("Another description")
                .build();
        String topic = "test-topic";
        Set<String> userRoles = Set.of("USER");
        List<UserDto> users = createTestUsers();
        EventMappingDto eventMapping = EventMappingDto.builder()
                .userRoles(userRoles)
                .build();

        when(eventMappingService.retrieveEventMappingByTopic(topic)).thenReturn(eventMapping);
        when(notificationService.retrieveUserIdsPerUserRolesAndOrganization(userRoles, "ANOTHER_ORG"))
                .thenReturn(users);

        // When
        kafkaMessageHandler.consume(validEvent, topic);

        // Then
        ArgumentCaptor<NotificationDto> notificationCaptor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).createNotificationsForEachUser(any(List.class), notificationCaptor.capture());

        NotificationDto capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getNotificationStatus()).isEqualTo(NotificationStatus.UNREAD.toString());
        assertThat(capturedNotification.getSourceComponent()).isEqualTo("AnotherComponent");
        assertThat(capturedNotification.getPriority()).isEqualTo(Priority.MID.toString());
        assertThat(capturedNotification.getDescription()).isEqualTo("Another description");
        assertThat(capturedNotification.getType()).isEqualTo("ANOTHER_EVENT");
        assertThat(capturedNotification.getUser()).isNull();
        assertThat(capturedNotification.getUserId()).isNull();
        assertThat(capturedNotification.getTimestamp()).isNotNull();
    }

    private EventDto createValidEvent() {
        return EventDto.builder()
                .priority(Priority.HIGH.toString())
                .sourceComponent("TestComponent")
                .organization("TEST_ORG")
                .type("TEST_EVENT")
                .description("Test description")
                .timestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC).toString())
                .data(null)
                .build();
    }

    private List<UserDto> createTestUsers() {
        List<UserDto> users = new ArrayList<>();
        for (int i = 0; i < 2; i++)
            users.add(createUserDto(i));
        return users;
    }

    private UserDto createUserDto(int i) {
        return UserDto.builder()
                .userId("id-" + i)
                .username("user-" + i)
                .email("mock-email-" + i)
                .firstName("first-" + i)
                .lastName("last-" + i)
                .userRole("USER")
                .pilotRole("TEST")
                .pilotCode("TEST")
                .build();
    }
}
