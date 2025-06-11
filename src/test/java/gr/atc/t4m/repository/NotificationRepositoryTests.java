package gr.atc.t4m.repository;

import gr.atc.t4m.TestcontainersConfiguration;
import gr.atc.t4m.enums.NotificationStatus;
import gr.atc.t4m.enums.Priority;
import gr.atc.t4m.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Notification Repository Tests")
class NotificationRepositoryTests {

    @Autowired
    private NotificationRepository notificationRepository;

    private Notification testNotification1;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        notificationRepository.deleteAll();

        // Create test notifications
        testNotification1 = new Notification();
        testNotification1.setUserId("Test User 1");
        testNotification1.setUser("Test User");
        testNotification1.setDescription("Test Description 1");
        testNotification1.setNotificationStatus(NotificationStatus.READ.toString());
        testNotification1.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        testNotification1.setSourceComponent("Test Component");
        testNotification1.setPriority(Priority.MID.toString());
        testNotification1.setType("Type1");

        Notification testNotification1Unread = new Notification();
        testNotification1Unread.setUserId("Test User 1");
        testNotification1Unread.setUser("Test User");
        testNotification1Unread.setDescription("Test Description 1 - Unread");
        testNotification1Unread.setNotificationStatus(NotificationStatus.UNREAD.toString());
        testNotification1Unread.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        testNotification1Unread.setSourceComponent("Test Component");
        testNotification1Unread.setPriority(Priority.MID.toString());
        testNotification1Unread.setType("Type1");

        Notification testNotification2 = new Notification();
        testNotification2.setUserId("Test User 2");
        testNotification2.setUser("Test User");
        testNotification2.setDescription("Test Description 2");
        testNotification2.setNotificationStatus(NotificationStatus.READ.toString());
        testNotification2.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        testNotification2.setSourceComponent("Test Component");
        testNotification2.setPriority(Priority.MID.toString());
        testNotification2.setType("Type2");

        Notification testNotification3 = new Notification();
        testNotification3.setUserId("SUPER_ADMIN");
        testNotification3.setUser("SUPER_ADMIN");
        testNotification3.setDescription("Test Description 3");
        testNotification3.setNotificationStatus(NotificationStatus.READ.toString());
        testNotification3.setTimestamp(LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC));
        testNotification3.setSourceComponent("Test Component");
        testNotification3.setPriority(Priority.MID.toString());
        testNotification3.setType("Type3");

        // Save test data
        testNotification1 = notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification1Unread);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);
    }
    
    @DisplayName("Save notification : Success")
    @Test
    void givenNotification_whenSave_thenReturnSavedNotification() {
        // Given
        Notification newNotification = Notification.builder()
                .description("New Notification")
                .userId("newUser")
                .notificationStatus("UNREAD")
                .timestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC))
                .build();

        // When
        Notification savedNotification = notificationRepository.save(newNotification);

        // Then
        assertThat(savedNotification.getId()).isNotNull();
        assertThat(savedNotification.getUserId()).isEqualTo("newUser");

        // Verify it can be retrieved
        Optional<Notification> retrieved = notificationRepository.findById(savedNotification.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getDescription()).isEqualTo("New Notification");
    }

    @DisplayName("Retrieve all Notifications : Success")
    @Test
    void whenFindAll_thenReturnAllNotifications() {
        // When
        List<Notification> allNotifications = notificationRepository.findAll();

        // Then
        assertThat(allNotifications).hasSize(4);
        assertThat(allNotifications)
                .extracting(Notification::getDescription)
                .containsExactlyInAnyOrder(
                        "Test Description 1",
                        "Test Description 2",
                        "Test Description 3",
                        "Test Description 1 - Unread"
                );
    }

    @DisplayName("Delete notification by ID : Success")
    @Test
    void givenNotificationId_whenDeleteById_thenNotificationDeleted() {
        // Given
        Long notificationId = testNotification1.getId();

        // When
        notificationRepository.deleteById(notificationId);

        // Then
        Optional<Notification> deleted = notificationRepository.findById(notificationId);
        assertThat(deleted).isEmpty();

        // Verify other notifications still exist
        assertThat(notificationRepository.findAll()).hasSize(3);
    }

    @DisplayName("Retrieve notifications by user ID : Success")
    @Test
    void givenUserId_whenFindByUserId_thenReturnNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());

        // When
        Page<Notification> result = notificationRepository.findByUserId("Test User 1", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();

        // Verify correct notifications are returned
        assertThat(result.getContent())
                .extracting(Notification::getUserId)
                .containsOnly("Test User 1");

        assertThat(result.getContent())
                .extracting(Notification::getDescription)
                .containsExactlyInAnyOrder("Test Description 1", "Test Description 1 - Unread");
    }

    @DisplayName("Retrieve unread notifications by user ID : Success")
    @Test
    void givenUserId_whenFindByUserIdAndNotificationStatus_thenReturnUnreadNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Notification> result = notificationRepository
                .findByUserIdAndNotificationStatus("Test User 1", "Unread", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        Notification unreadNotification = result.getContent().getFirst();
        assertThat(unreadNotification.getUserId()).isEqualTo("Test User 1");
        assertThat(unreadNotification.getNotificationStatus()).isEqualTo("Unread");
        assertThat(unreadNotification.getDescription()).isEqualTo("Test Description 1 - Unread");
    }

    @DisplayName("Retrieve read notifications by user ID : Success")
    @Test
    void givenUserId_whenFindByUserIdAndNotificationStatus_thenReturnReadNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Notification> result = notificationRepository
                .findByUserIdAndNotificationStatus("Test User 1", "Read", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        Notification readNotification = result.getContent().getFirst();
        assertThat(readNotification.getUserId()).isEqualTo("Test User 1");
        assertThat(readNotification.getNotificationStatus()).isEqualTo("Read");
        assertThat(readNotification.getDescription()).isEqualTo("Test Description 1");
    }

    @DisplayName("Retrieve empty page when no notifications match criteria : Success")
    @Test
    void shouldReturnEmptyPageWhenNoMatchingNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Notification> result = notificationRepository
                .findByUserIdAndNotificationStatus("Test User 1", "MOCK", pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @DisplayName("Update notification status : Success")
    @Test
    void whenUpdateNotification_thenSuccess() {
        // When
        Optional<Notification> notificationOpt = notificationRepository.findById(testNotification1.getId());
        assertThat(notificationOpt).isPresent();

        Notification notification = notificationOpt.get();
        notification.setNotificationStatus("Unread");
        notificationRepository.save(notification);

        // Then
        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> unreadResult = notificationRepository
                .findByUserIdAndNotificationStatus("Test User 1", "Unread", pageable);
        assertThat(unreadResult.getContent()).hasSize((2));

        Page<Notification> readResult = notificationRepository
                .findByUserIdAndNotificationStatus("Test User 1", "Read", pageable);
        assertThat(readResult.getContent()).isEmpty();
    }
}