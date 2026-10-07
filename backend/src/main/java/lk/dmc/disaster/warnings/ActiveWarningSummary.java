package lk.dmc.disaster.warnings;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;

/** Snapshot of an ACTIVE warning as other modules see it. */
public record ActiveWarningSummary(
    UUID warningId,
    WarningLevel level,
    String title,
    String instructions,
    UUID eventId,
    Instant issuedAt,
    Set<UUID> districtIds,
    Set<UUID> riverBasinIds) {

  public ActiveWarningSummary {
    districtIds = Set.copyOf(districtIds);
    riverBasinIds = Set.copyOf(riverBasinIds);
  }
}
