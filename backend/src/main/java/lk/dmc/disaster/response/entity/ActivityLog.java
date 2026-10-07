package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Column(name = "event_id")
  private UUID eventId;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "type", nullable = false, length = 15)
  private ActivityType type;

  @Column(name = "message", nullable = false, length = 200)
  private String message;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  public static ActivityLog create(
      UUID districtId,
      UUID eventId,
      ActivityType type,
      String message,
      Instant occurredAt) {
    ActivityLog log = new ActivityLog();
    log.id = UUID.randomUUID();
    log.districtId = districtId;
    log.eventId = eventId;
    log.type = type;
    log.message = message;
    log.occurredAt = occurredAt;
    return log;
  }
}
