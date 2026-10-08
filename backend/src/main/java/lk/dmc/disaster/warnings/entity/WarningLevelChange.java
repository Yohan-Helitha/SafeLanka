package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One step in a warning's level history: the level it was issued at (no previous level), or a
 * raise from one level to a higher one.
 */
@Entity
@Table(name = "warning_level_changes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarningLevelChange {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_level")
  private WarningLevel fromLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_level", nullable = false)
  private WarningLevel toLevel;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** The level a warning was first issued at. */
  static WarningLevelChange issuedAt(WarningLevel level, UUID changedBy, Instant at) {
    return of(null, level, changedBy, at);
  }

  /** A raise from one level to a higher one. */
  static WarningLevelChange raised(
      WarningLevel from, WarningLevel to, UUID changedBy, Instant at) {
    return of(from, to, changedBy, at);
  }

  private static WarningLevelChange of(
      WarningLevel from, WarningLevel to, UUID changedBy, Instant at) {
    WarningLevelChange change = new WarningLevelChange();
    change.id = UUID.randomUUID();
    change.fromLevel = from;
    change.toLevel = to;
    change.changedBy = changedBy;
    change.changedAt = at;
    return change;
  }
}
