package gr.atc.t4m.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import gr.atc.t4m.controller.responses.BaseAppResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import static gr.atc.t4m.exception.CustomExceptions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "Validation error";

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> invalidSecurityException(@NotNull AccessDeniedException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Invalid authorization parameters. You don't have the rights to access the resource or check the JWT and CSRF Tokens", ex.getCause()),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> handleDtoValidationExceptions(@NotNull MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(BaseAppResponse.error("Validation failed", errors),
                HttpStatus.BAD_REQUEST);
    }

    /*
     * Handles missing request body or missing data in request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseAppResponse<String>> handleHttpMessageNotReadableExceptionHandler(
            HttpMessageNotReadableException ex) {
        String errorMessage = "Required request body is missing or includes invalid data";

        // Check if instance is for InvalidFormat Validation
        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx
                && invalidFormatEx.getTargetType().isEnum()) {
            String fieldName = invalidFormatEx.getPath().getFirst().getFieldName();
            String invalidValue = invalidFormatEx.getValue().toString();

            // Format the error message according to the Validation Type failure
            errorMessage = String.format("Invalid value '%s' for field '%s'. Allowed values are: %s",
                    invalidValue, fieldName, Arrays.stream(invalidFormatEx.getTargetType().getEnumConstants())
                            .map(Object::toString).collect(Collectors.joining(", ")));

        }
        // Generic error handling
        return ResponseEntity.badRequest().body(BaseAppResponse.error(VALIDATION_ERROR, errorMessage));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseAppResponse<Map<String, String>>> handleParameterValidationExceptions(@NotNull ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String paramName = violation.getPropertyPath().toString();
            // Remove the method name from the path
            paramName = paramName.substring(paramName.lastIndexOf('.') + 1);
            String message = violation.getMessage();
            errors.put(paramName, message);
        });

        return new ResponseEntity<>(BaseAppResponse.error("Validation failed", errors),
                HttpStatus.BAD_REQUEST);
    }

    /*
     * Handles validation for Method Parameters
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<BaseAppResponse<String>> validationExceptionHandler(
            @NonNull HandlerMethodValidationException ex) {
        return new ResponseEntity<>(BaseAppResponse.error(VALIDATION_ERROR, "Invalid input field"),
                HttpStatus.BAD_REQUEST);
    }

    /*
     * Handles general validation exception
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<BaseAppResponse<String>> handleGeneralValidationException(
            @NonNull ValidationException ex) {
        return new ResponseEntity<>(BaseAppResponse.error(VALIDATION_ERROR, ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseAppResponse<String>> handleGeneralException(@NotNull Exception ex) {
        return new ResponseEntity<>(BaseAppResponse.error("An unexpected error occurred", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseAppResponse<String>> handleResourceNotFoundException(@NotNull ResourceNotFoundException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Requested resource not found in DB", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ModelMappingException.class)
    public ResponseEntity<BaseAppResponse<String>> handleModelMappingException(@NotNull ModelMappingException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Internal error in mapping process", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<BaseAppResponse<String>> handleForbiddenAccessException(@NotNull ForbiddenAccessException ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Unauthorized action", ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceAlreadyExists.class)
    public ResponseEntity<BaseAppResponse<String>> handleResourceAlreadyExists(@NotNull ResourceAlreadyExists ex) {
        return new ResponseEntity<>(BaseAppResponse.error("Resource already exists", ex.getMessage()), HttpStatus.CONFLICT);
    }
}

