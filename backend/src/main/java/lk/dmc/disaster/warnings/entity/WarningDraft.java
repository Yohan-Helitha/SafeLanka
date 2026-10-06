package lk.dmc.disaster.warnings.entity;

import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;

/**
 * Everything an officer decides when issuing a warning. Keeps {@link Warning#publish} to three
 * parameters.
 *
 * @param hazardId the hazard being warned about
 * @param eventId the disaster event it belongs to, or null
 * @param evidenceReportIds verified reports linked as evidence, may be empty
 */
public record WarningDraft(
    UUID hazardId,
    UUID eventId,
    WarningLevel level,
    WarningTarget target,
    WarningContent content,
    Set<UUID> evidenceReportIds) {

  public WarningDraft {
    evidenceReportIds = evidenceReportIds == null ? Set.of() : Set.copyOf(evidenceReportIds);
  }
}
