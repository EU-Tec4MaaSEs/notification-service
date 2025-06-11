package gr.atc.t4m.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserManagerResponse{
    private List<UserDto> data;
    private Object errors;
    private String message;
    private boolean success;
    private String timestamp;
}