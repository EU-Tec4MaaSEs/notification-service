package gr.atc.t4m.validation.validators;

import gr.atc.t4m.validation.ValidNotificationStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.EnumUtils;

import gr.atc.t4m.enums.NotificationStatus;

public class NotificationStatusValidator implements ConstraintValidator<ValidNotificationStatus, String> {

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        if (status == null)
            return true;

        return EnumUtils.isValidEnumIgnoreCase(NotificationStatus.class, status);
    }
}
