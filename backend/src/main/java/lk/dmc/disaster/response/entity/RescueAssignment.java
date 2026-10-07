package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "rescue_assignments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RescueAssignment {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "warning_id")
  private UUID warningId;

  @Column(name = "team_id")
  private UUID teamId;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "latitude", nullable = false)
  private double latitude;

  @Column(name = "longitude", nullable = false)
  private double longitude;

  @Column(name = "location_text", nullable = false, length = 200)
  private String locationText;

  @Column(name = "task", nullable = false, length = 500)
  private String task;

  @Column(name = "priority", nullable = false)
  private short priority;

  @Column(name = "people_estimated", nullable = false)
  private int peopleEstimated;

  @Column(name = "destination_shelter_id")
  private UUID destinationShelterId;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AssignmentStatus status;

  @Column(name = "decline_reason", length = 200)
  private String declineReason;

  @Column(name = "assigned_at")
  private Instant assignedAt;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "version", nullable = false)
  private long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static RescueAssignment create(
      UUID eventId,
      UUID warningId,
      UUID createdBy,
      double latitude,
      double longitude,
      String locationText,
      String task,
      short priority,
      int peopleEstimated,
      UUID destinationShelterId) {
    RescueAssignment a = new RescueAssignment();
    a.id = UUID.randomUUID();
    a.eventId = eventId;
    a.warningId = warningId;
    a.createdBy = createdBy;
    a.latitude = latitude;
    a.longitude = longitude;
    a.locationText = locationText;
    a.task = task;
    a.priority = priority;
    a.peopleEstimated = peopleEstimated;
    a.destinationShelterId = destinationShelterId;
    a.status = AssignmentStatus.UNASSIGNED;
    a.version = 0L;
    return a;
  }

  public void assignTeam(UUID teamId) {
    this.teamId = teamId;
    this.status = AssignmentStatus.PENDING_ACK;
    this.assignedAt = Instant.now();
  }

  public void acknowledge() {
    this.status = AssignmentStatus.ACCEPTED;
    this.acknowledgedAt = Instant.now();
  }

  public void startEnRoute() {
    this.status = AssignmentStatus.EN_ROUTE;
  }

  public void startActive() {
    this.status = AssignmentStatus.ACTIVE;
  }

  public void complete() {
    this.status = AssignmentStatus.COMPLETED;
    this.completedAt = Instant.now();
  }

  public void cancel(String reason) {
    this.status = AssignmentStatus.CANCELLED;
    this.declineReason = reason;
  }

  public void decline(String reason) {
    this.status = AssignmentStatus.UNASSIGNED;
    this.teamId = null;
    this.declineReason = reason;
  }

  public void respond(boolean accept, String declineReason) {
    if (accept) {
      acknowledge();
    } else {
      decline(declineReason);
    }
  }
}
