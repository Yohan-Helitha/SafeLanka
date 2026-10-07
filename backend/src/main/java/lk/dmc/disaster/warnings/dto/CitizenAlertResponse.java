package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.service.CitizenAlert;

/**
 * An alert as a citizen sees it.
 *
 * @param districtId the citizen district the alert reached them in
 * @param riverBasinId the citizen river basin, or null
 * @param audible true when the app should also sound the alarm
 */
public record CitizenAlertResponse(
    UUID warningId,
    WarningLevel level,
    String title,
    String message,
    String instructions,
    Instant issuedAt,
    UUID districtId,
    UUID riverBasinId,
    boolean audible) {

  public static CitizenAlertResponse from(CitizenAlert alert) {
    return new CitizenAlertResponse(
        alert.warningId(),
        alert.level(),
        alert.title(),
        alert.message(),
        alert.instructions(),
        alert.issuedAt(),
        alert.districtId(),
        alert.riverBasinId(),
        alert.audible());
  }
}
