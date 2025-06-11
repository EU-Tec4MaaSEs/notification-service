package gr.atc.t4m.service.interfaces;

public interface IWebSocketService {

    void notifyUsersAndRolesViaWebSocket(String message, String topicName);

    void notifyUserViaWebSocket(String userId, String message);
}
