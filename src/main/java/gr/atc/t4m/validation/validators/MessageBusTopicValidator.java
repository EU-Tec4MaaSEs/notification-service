package gr.atc.t4m.validation.validators;

import gr.atc.t4m.enums.MessageBusTopic;
import gr.atc.t4m.validation.ValidMessageBusTopic;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MessageBusTopicValidator implements ConstraintValidator<ValidMessageBusTopic, String> {

    @Override
    public boolean isValid(String topic, ConstraintValidatorContext context) {
        if (topic == null)
            return true;

        return MessageBusTopic.isValidTopic(topic);
    }
}
