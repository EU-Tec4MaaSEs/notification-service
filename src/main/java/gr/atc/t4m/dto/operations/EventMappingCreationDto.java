package gr.atc.t4m.dto.operations;

import java.util.Set;

import gr.atc.t4m.validation.ValidMessageBusTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record EventMappingCreationDto(

    @Schema(description = "Topic for the event mapping", example = "negotiation-started", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Topic cannot be empty")
    @ValidMessageBusTopic
    String topic,

    @Schema(description = "Optional description for the event mapping", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String description,

    @Schema(description = "List of User Roles that will receive notifications upon new event. To include all roles provide: List.of('ALL')", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "User Roles cannot be empty")
    Set<String> userRoles
) {}
