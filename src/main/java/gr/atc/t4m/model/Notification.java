package gr.atc.t4m.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name="user_id", length=50, nullable=false)
    private String userId;

    @Column(name="user_name", length=50, nullable=false)
    private String user;

    @Column(name="notification_status", length=10, nullable=false)
    private String notificationStatus;

    @Column(name="source_component", length=30, nullable=false)
    private String sourceComponent;

    @Column(name="type", length=30, nullable=false)
    private String type;

    @Column(name = "description", length=200)
    private String description;

    @Column(name = "timestamp", nullable=false)
    private OffsetDateTime timestamp;

    @Column(name = "priority", length=10)
    private String priority;
}
