package lk.dmc.disaster.response.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LongitudeValidator implements ConstraintValidator<ValidLongitude, Number> {

  @Override
  public boolean isValid(Number value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    double lng = value.doubleValue();
    return lng >= -180.0 && lng <= 180.0 && !Double.isNaN(lng) && !Double.isInfinite(lng);
  }
}
