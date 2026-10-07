package lk.dmc.disaster.response.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lk.dmc.disaster.response.entity.RescueTeamStatus;

public class TeamStatusValidator implements ConstraintValidator<ValidTeamStatus, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true; // Use @NotBlank to reject null/blank
    }
    try {
      RescueTeamStatus.valueOf(value.trim());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
