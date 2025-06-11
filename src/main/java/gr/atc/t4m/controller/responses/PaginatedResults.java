package gr.atc.t4m.controller.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Paginated Result Data Transfer Object", title = "Paginated Results")
public class PaginatedResults<T> {

    @JsonProperty("results")
    private List<T> results;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("lastPage")
    private Boolean lastPage;
}
