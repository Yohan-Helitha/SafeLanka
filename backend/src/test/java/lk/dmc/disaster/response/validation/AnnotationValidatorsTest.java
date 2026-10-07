package lk.dmc.disaster.response.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnnotationValidatorsTest {

  @Test
  void latitudeValidator_validatesCorrectly() {
    LatitudeValidator validator = new LatitudeValidator();

    // Null is considered valid (handled by @NotNull if required)
    assertThat(validator.isValid(null, null)).isTrue();

    // Valid boundaries and typical values
    assertThat(validator.isValid(-90.0, null)).isTrue();
    assertThat(validator.isValid(90.0, null)).isTrue();
    assertThat(validator.isValid(0.0, null)).isTrue();
    assertThat(validator.isValid(6.9271, null)).isTrue();

    // Out of bounds
    assertThat(validator.isValid(-90.0001, null)).isFalse();
    assertThat(validator.isValid(90.0001, null)).isFalse();
    assertThat(validator.isValid(Double.NaN, null)).isFalse();
    assertThat(validator.isValid(Double.POSITIVE_INFINITY, null)).isFalse();
    assertThat(validator.isValid(Double.NEGATIVE_INFINITY, null)).isFalse();
  }

  @Test
  void longitudeValidator_validatesCorrectly() {
    LongitudeValidator validator = new LongitudeValidator();

    // Null is considered valid
    assertThat(validator.isValid(null, null)).isTrue();

    // Valid boundaries and typical values
    assertThat(validator.isValid(-180.0, null)).isTrue();
    assertThat(validator.isValid(180.0, null)).isTrue();
    assertThat(validator.isValid(0.0, null)).isTrue();
    assertThat(validator.isValid(79.8612, null)).isTrue();

    // Out of bounds
    assertThat(validator.isValid(-180.0001, null)).isFalse();
    assertThat(validator.isValid(180.0001, null)).isFalse();
    assertThat(validator.isValid(Double.NaN, null)).isFalse();
    assertThat(validator.isValid(Double.POSITIVE_INFINITY, null)).isFalse();
    assertThat(validator.isValid(Double.NEGATIVE_INFINITY, null)).isFalse();
  }

  @Test
  void priorityValidator_validatesCorrectly() {
    PriorityValidator validator = new PriorityValidator();

    // Null is considered valid
    assertThat(validator.isValid(null, null)).isTrue();

    // Valid priorities (1, 2, 3)
    assertThat(validator.isValid(1, null)).isTrue();
    assertThat(validator.isValid(2, null)).isTrue();
    assertThat(validator.isValid(3, null)).isTrue();

    // Out of bounds
    assertThat(validator.isValid(0, null)).isFalse();
    assertThat(validator.isValid(4, null)).isFalse();
    assertThat(validator.isValid(-1, null)).isFalse();
  }

  @Test
  void teamStatusValidator_validatesCorrectly() {
    TeamStatusValidator validator = new TeamStatusValidator();

    // Null or blank are considered valid (handled by @NotBlank)
    assertThat(validator.isValid(null, null)).isTrue();
    assertThat(validator.isValid("", null)).isTrue();
    assertThat(validator.isValid("   ", null)).isTrue();

    // Valid statuses
    assertThat(validator.isValid("AVAILABLE", null)).isTrue();
    assertThat(validator.isValid("DISPATCHED", null)).isTrue();
    assertThat(validator.isValid("EN_ROUTE", null)).isTrue();
    assertThat(validator.isValid("ACTIVE", null)).isTrue();
    assertThat(validator.isValid("OFFLINE_UNKNOWN", null)).isTrue();

    // Invalid status string
    assertThat(validator.isValid("NON_EXISTENT_STATUS", null)).isFalse();
    assertThat(validator.isValid("available", null)).isFalse();
  }
}
