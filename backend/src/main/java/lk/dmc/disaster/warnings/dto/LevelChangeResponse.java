package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.warnings.entity.WarningLevelChange;

/**
 * One step in a warning's level history.
 *
 * @param fromLevel the level before the change, or null for the level the warning was issued at
 * @param toLevel the level after the change
 * @param changedBy the officer who issued or raised it
 */
public record LevelChangeResponse(
    WarningLevel fromLevel, WarningLevel toLevel, UUID changedBy, Instant changedAt) {

  /** Copies the fields of one history step. */
  public static LevelChangeResponse from(WarningLevelChange change) {
    return new LevelChangeResponse(
        change.getFromLevel(), change.getToLevel(), change.getChangedBy(), change.getChangedAt());
  }
}
