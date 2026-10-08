package lk.dmc.disaster.warnings;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;

/** Published when a warning is raised to a higher level; it is still the same warning. */
public record WarningEscalatedEvent(
    UUID warningId,
    WarningLevel oldLevel,
    WarningLevel newLevel,
    Set<UUID> resolvedDistrictIds,
    Instant escalatedAt) {

  public WarningEscalatedEvent {
    resolvedDistrictIds = Set.copyOf(resolvedDistrictIds);
  }
}
