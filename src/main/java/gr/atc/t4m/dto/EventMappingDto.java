package gr.atc.t4m.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.t4m.validation.ValidMessageBusTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event Mapping Object Representation", title = "Event Mapping")
public class EventMappingDto {

    @Schema(description = "Event Mapping ID")
    @JsonProperty("eventMappingId")
    private Long id;

    @Schema(description = "Message Bus topic name")
    @NotEmpty(message = "Topic cannot be empty")
    @ValidMessageBusTopic
    private String topic;

    @Schema(description = "Optional description for the event mapping")
    private String description;

    @Schema(description = "Set of UserRoles that should receive the notification per topic. List.of('ALL') correlates to all user roles")
    @NotEmpty(message = "User Roles cannot be empty")
    private Set<String> userRoles;
}
