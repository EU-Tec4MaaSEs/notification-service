package gr.atc.t4m.validation;

import gr.atc.t4m.validation.validators.PriorityValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PriorityValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPriority {
    String message() default "Invalid priority. Only 'Low', 'Mid' and 'High' are allowed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}