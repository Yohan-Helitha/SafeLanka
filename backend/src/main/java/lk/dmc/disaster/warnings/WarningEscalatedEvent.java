package lk.dmc.disaster.warnings;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;

/** Published when a warning is replaced by a higher-level one. */
public record WarningEscalatedEvent(
    UUID previousWarningId,
    UUID newWarningId,
    WarningLevel oldLevel,
    WarningLevel newLevel,
    Set<UUID> resolvedDistrictIds,
    Instant escalatedAt) {

  public WarningEscalatedEvent {
    resolvedDistrictIds = Set.copyOf(resolvedDistrictIds);
  }
}
