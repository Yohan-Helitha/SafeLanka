package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "occupancy_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OccupancyLog {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "shelter_id", nullable = false)
  private UUID shelterId;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "occupancy", nullable = false)
  private int occupancy;

  @Column(name = "delta", nullable = false)
  private int delta;

  @Column(name = "recorded_by", nullable = false)
  private UUID recordedBy;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  public static OccupancyLog create(
      UUID shelterId,
      UUID eventId,
      int occupancy,
      int delta,
      UUID recordedBy,
      Instant recordedAt) {
    OccupancyLog log = new OccupancyLog();
    log.id = UUID.randomUUID();
    log.shelterId = shelterId;
    log.eventId = eventId;
    log.occupancy = occupancy;
    log.delta = delta;
    log.recordedBy = recordedBy;
    log.recordedAt = recordedAt;
    return log;
  }
}
