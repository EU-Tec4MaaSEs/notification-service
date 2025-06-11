package gr.atc.t4m.validation.validators;

import gr.atc.t4m.enums.Priority;
import gr.atc.t4m.validation.ValidPriority;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class PriorityValidator implements ConstraintValidator<ValidPriority, String> {
    @Override
    public boolean isValid(String priority, ConstraintValidatorContext context) {
        if (priority == null)
            return true;

       return EnumUtils.isValidEnumIgnoreCase(Priority.class, priority);
    }
}
