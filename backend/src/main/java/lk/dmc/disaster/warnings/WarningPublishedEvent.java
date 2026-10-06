package lk.dmc.disaster.warnings;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;

/** Published after a warning is saved and sent. Feeds the district activity log. */
public record WarningPublishedEvent(
    UUID warningId,
    WarningLevel level,
    UUID eventId,
    Set<UUID> districtIds,
    Set<UUID> riverBasinIds,
    Set<UUID> resolvedDistrictIds,
    Instant issuedAt) {

  public WarningPublishedEvent {
    districtIds = Set.copyOf(districtIds);
    riverBasinIds = Set.copyOf(riverBasinIds);
    resolvedDistrictIds = Set.copyOf(resolvedDistrictIds);
  }
}
