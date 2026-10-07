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
@Table(name = "team_status_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamStatusLog {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "team_id", nullable = false)
  private UUID teamId;

  @Column(name = "assignment_id")
  private UUID assignmentId;

  @Column(name = "from_status", nullable = false, length = 20)
  private String fromStatus;

  @Column(name = "to_status", nullable = false, length = 20)
  private String toStatus;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  @Column(name = "recorded_offline", nullable = false)
  private boolean recordedOffline;

  @Column(name = "client_ref")
  private UUID clientRef;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  public static TeamStatusLog create(
      UUID teamId,
      UUID assignmentId,
      String fromStatus,
      String toStatus,
      UUID changedBy,
      Instant changedAt,
      boolean recordedOffline,
      UUID clientRef) {
    TeamStatusLog log = new TeamStatusLog();
    log.id = UUID.randomUUID();
    log.teamId = teamId;
    log.assignmentId = assignmentId;
    log.fromStatus = fromStatus;
    log.toStatus = toStatus;
    log.changedBy = changedBy;
    log.changedAt = changedAt;
    log.recordedOffline = recordedOffline;
    log.clientRef = clientRef;
    log.syncedAt = Instant.now();
    return log;
  }
}
