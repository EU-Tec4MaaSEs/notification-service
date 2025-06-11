package gr.atc.t4m.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "event_mappings")
public class EventMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name="topic", length=50, nullable=false, unique=true)
    private String topic;

    @Column(name="description", length=200, nullable=true)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_mapping_user_roles",
            joinColumns = @JoinColumn(name = "event_mapping_id")
    )
    @Column(name = "user_roles")
    private Set<String> userRoles;
}
