package gr.atc.t4m.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(hidden = true)
@Builder
public record UserDto(
        String userId,

        String username,

        String firstName,

        String lastName,

        String email,

        String pilotRole,

        String pilotCode,

        String userRole
) { }
