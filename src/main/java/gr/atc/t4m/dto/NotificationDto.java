package gr.atc.t4m.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.t4m.validation.ValidNotificationStatus;
import gr.atc.t4m.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification Object Representation", title = "Notification")
public class NotificationDto {

    @Schema(description = "ID of Notification")
    @JsonProperty("notificationId")
    private Long id;

    @Schema(description = "Recipient User's ID")
    private String userId;

    @Schema(description = "Recipient User' Full Name")
    private String user;

    @Schema(description = "Notifications Status", allowableValues="Read, Unread")
    @ValidNotificationStatus
    private String notificationStatus;

    @Schema(description = "Source Component that generated the notification")
    private String sourceComponent;

    @Schema(description = "Type of notification")
    private String type;

    @Schema(description = "Description of the notification")
    private String description;

    @Schema(description = "Timestamp of Notification" , example="2000-01-01T00:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private OffsetDateTime timestamp;

    @Schema(description = "Priority of the notification", allowableValues = "High, Medium, Low")
    @ValidPriority
    private String priority;
}

