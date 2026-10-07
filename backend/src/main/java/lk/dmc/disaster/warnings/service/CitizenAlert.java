package lk.dmc.disaster.warnings.service;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningRules;

/**
 * An alert as a citizen sees it.
 *
 * @param districtId the citizen district
 * @param riverBasinId the citizen river basin, or null
 * @param audible true when the app should also sound the alarm
 */
public record CitizenAlert(
    UUID warningId,
    WarningLevel level,
    String title,
    String message,
    String instructions,
    Instant issuedAt,
    UUID districtId,
    UUID riverBasinId,
    boolean audible) {

  static CitizenAlert of(Warning warning, UUID districtId, UUID riverBasinId) {
    return new CitizenAlert(
        warning.getId(),
        warning.getLevel(),
        warning.getTitle(),
        warning.getMessage(),
        warning.getInstructions(),
        warning.getIssuedAt(),
        districtId,
        riverBasinId,
        WarningRules.audibleAllowedAt(warning.getLevel()));
  }
}
