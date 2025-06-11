package gr.atc.t4m.controller.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseAppResponse<T> {
    private T data;
    private Object errors;
    private String message;
    private boolean success;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Builder.Default
    private OffsetDateTime timestamp = LocalDateTime.now().withNano(0).atOffset(ZoneOffset.UTC);

    public static <T> BaseAppResponse<T> success(T data) {
        return BaseAppResponse.<T>builder()
                .success(true)
                .message("Operation successful")
                .data(data)
                .build();
    }

    public static <T> BaseAppResponse<T> success(T data, String message) {
        return BaseAppResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseAppResponse<T> error(String message) {
        return BaseAppResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> BaseAppResponse<T> error(String message, Object errors) {
        return BaseAppResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
}