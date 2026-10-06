package lk.dmc.disaster.warnings.entity;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/** Business constants and the text-length check for the warnings module. */
public final class WarningRules {

  public static final int TITLE_MIN = 5;
  public static final int TITLE_MAX = 80;
  public static final int MESSAGE_MIN = 10;
  public static final int MESSAGE_MAX = 1000;
  public static final int SMS_MIN = 10;
  public static final int SMS_MAX = 160;
  public static final int INSTRUCTIONS_MIN = 5;
  public static final int INSTRUCTIONS_MAX = 300;
  public static final int CANCEL_REASON_MIN = 5;
  public static final int CANCEL_REASON_MAX = 300;
  public static final int HAZARD_DESCRIPTION_MIN = 10;
  public static final int HAZARD_DESCRIPTION_MAX = 500;
  public static final int FAILURE_REASON_MAX = 200;

  public static final int SEVERITY_MIN = 1;
  public static final int SEVERITY_MAX = 5;

  /** Severity given to a hazard created from a verified report. */
  public static final int REPORT_HAZARD_SEVERITY = 2;

  /** Severity given to a hazard created when a gauge crosses its alert level. */
  public static final int SENSOR_HAZARD_SEVERITY = 3;

  /** Severity a hazard is raised to when a gauge reaches major flood level. */
  public static final int MAJOR_FLOOD_SEVERITY = 4;

  /** Lowest level that also triggers the audible channel. */
  public static final WarningLevel AUDIBLE_MIN_LEVEL = WarningLevel.WARNING;

  /** How far back verified reports count as evidence for a hazard. */
  public static final Duration EVIDENCE_LOOKBACK = Duration.ofHours(72);

  /** One simulated tick moves a gauge by (major - alert) divided by this value. */
  public static final int SENSOR_STEP_DIVISOR = 8;

  public static final BigDecimal SENSOR_MIN_STEP = new BigDecimal("0.05");

  private WarningRules() {}

  /** True when the audible channel may be used for a warning of this level. */
  public static boolean audibleAllowedAt(WarningLevel level) {
    return !AUDIBLE_MIN_LEVEL.isHigherThan(level);
  }

  /**
   * Trims the text and checks its length.
   *
   * @throws AppException VALIDATION_ERROR naming the field when the text is missing or out of range
   */
  public static String requireText(String field, String value, int min, int max) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.length() < min || trimmed.length() > max) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          field + " must be " + min + " to " + max + " characters.",
          Map.of("field", field));
    }
    return trimmed;
  }

  /**
   * Checks a severity is within 1 to 5.
   *
   * @throws AppException VALIDATION_ERROR when out of range
   */
  public static int requireSeverity(int severity) {
    if (severity < SEVERITY_MIN || severity > SEVERITY_MAX) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "severity must be " + SEVERITY_MIN + " to " + SEVERITY_MAX + ".",
          Map.of("field", "severity"));
    }
    return severity;
  }
}
