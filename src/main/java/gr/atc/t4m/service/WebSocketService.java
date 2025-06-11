package gr.atc.t4m.service;


import gr.atc.t4m.service.interfaces.IWebSocketService;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class WebSocketService implements IWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all users through WebSocket
     *
     * @param message : String message
     * @param topicName : Topic Name for specific user roles
     */
    @Async
    public void notifyUsersAndRolesViaWebSocket(String message, String topicName){
        try {
            String websocketTopic = "/topic/notifications/" + topicName;
            log.info("Notifying websocket topic: {}", websocketTopic);
            messagingTemplate.convertAndSend(websocketTopic, message);
        } catch (MessagingException e) {
            log.error("Error in sending data via websockets - {}", e.getMessage());
        }
    }

    /**
     * Notify specific user through WebSocket
     * Works when the UserID is mapped to a Spring Security Principal
     *
     * @param userId : User ID of user that will connect to specific topics
     * @param message : String message
     */
    @Async
    public void notifyUserViaWebSocket(String userId, String message) {
        try {
            String websocketTopic = "/user/" + userId + "/queue/notifications";
            log.info("Notifying user: {} on websocket topic: {}", userId, websocketTopic);
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message);
        } catch (MessagingException e) {
            log.error("Error in sending data to user via websockets - {}", e.getMessage());
        }
    }
}
