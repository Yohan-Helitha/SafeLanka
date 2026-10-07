package lk.dmc.disaster.warnings.integration;

import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Warning;

/**
 * What a gateway needs to reach one citizen. The texts are plain strings so a gateway checks its
 * own limits (such as the SMS length) instead of trusting the caller.
 */
public record DeliveryRequest(
    UUID warningId,
    UUID citizenId,
    WarningLevel level,
    String title,
    String message,
    String smsText) {

  /** Builds the request for one citizen from an issued warning. */
  public static DeliveryRequest of(Warning warning, UUID citizenId) {
    return new DeliveryRequest(
        warning.getId(),
        citizenId,
        warning.getLevel(),
        warning.getTitle(),
        warning.getMessage(),
        warning.getSmsText());
  }
}
