package lk.dmc.disaster.response.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PriorityValidator implements ConstraintValidator<ValidPriority, Number> {

  @Override
  public boolean isValid(Number value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // Use @NotNull for null checking
    }
    int p = value.intValue();
    return p >= 1 && p <= 3;
  }
}
