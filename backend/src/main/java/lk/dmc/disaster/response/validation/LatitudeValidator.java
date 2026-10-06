package lk.dmc.disaster.response.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LatitudeValidator implements ConstraintValidator<ValidLatitude, Number> {

  @Override
  public boolean isValid(Number value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    double lat = value.doubleValue();
    return lat >= -90.0 && lat <= 90.0 && !Double.isNaN(lat) && !Double.isInfinite(lat);
  }
}
