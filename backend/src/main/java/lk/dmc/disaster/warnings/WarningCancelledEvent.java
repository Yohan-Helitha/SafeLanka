package lk.dmc.disaster.warnings;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Published when an officer cancels an ACTIVE warning. */
public record WarningCancelledEvent(
    UUID warningId, String reason, Set<UUID> resolvedDistrictIds, Instant cancelledAt) {

  public WarningCancelledEvent {
    resolvedDistrictIds = Set.copyOf(resolvedDistrictIds);
  }
}
