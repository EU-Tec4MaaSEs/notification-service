package gr.atc.t4m.service.interfaces;

import gr.atc.t4m.dto.NotificationDto;
import gr.atc.t4m.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface INotificationService {
    void deleteNotificationById(Long notificationId, String userId);

    void updateNotificationStatusToRead(Long notificationId, String userId);

    Page<NotificationDto> retrieveAllNotificationsPerUserId(String userId, Pageable pageable);

    Page<NotificationDto> retrieveUnreadNotificationsPerUserId(String userId, Pageable pageable);

    NotificationDto retrieveNotificationById(Long notificationId, String userId);

    void createNotificationsForEachUser(List<UserDto> users, NotificationDto notification);

    List<UserDto> retrieveUserIdsPerOrganization(String organization);

    List<UserDto> retrieveUserIdsPerUserRolesAndOrganization(Set<String> userRoles, String organization);
}
