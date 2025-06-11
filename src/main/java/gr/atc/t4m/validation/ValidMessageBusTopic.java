package gr.atc.t4m.validation;

import gr.atc.t4m.validation.validators.MessageBusTopicValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MessageBusTopicValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMessageBusTopic {
    String message() default "Invalid message bus topic inserted.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
