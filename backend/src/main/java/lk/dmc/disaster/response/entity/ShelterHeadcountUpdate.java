package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shelter_headcount_updates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelterHeadcountUpdate {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "shelter_id", nullable = false)
  private UUID shelterId;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Column(name = "reported_occupancy", nullable = false)
  private int reportedOccupancy;

  @Column(name = "previous_occupancy")
  private Integer previousOccupancy;

  @Column(name = "reported_by_name", nullable = false, length = 120)
  private String reportedByName;

  @Column(name = "reported_by_role", nullable = false, length = 60)
  private String reportedByRole;

  @Column(name = "message", nullable = false, length = 255)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private HeadcountUpdateStatus status;

  @Column(name = "reported_at", nullable = false)
  private Instant reportedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "processed_by")
  private UUID processedBy;

  public static ShelterHeadcountUpdate create(
      UUID shelterId,
      UUID districtId,
      int reportedOccupancy,
      Integer previousOccupancy,
      String reportedByName,
      String reportedByRole,
      String message) {
    ShelterHeadcountUpdate update = new ShelterHeadcountUpdate();
    update.id = UUID.randomUUID();
    update.shelterId = shelterId;
    update.districtId = districtId;
    update.reportedOccupancy = reportedOccupancy;
    update.previousOccupancy = previousOccupancy;
    update.reportedByName = reportedByName;
    update.reportedByRole = reportedByRole != null ? reportedByRole : "SHELTER_COORDINATOR";
    update.message = message;
    update.status = HeadcountUpdateStatus.PENDING;
    update.reportedAt = Instant.now();
    return update;
  }

  public void markApplied(UUID processorId) {
    this.status = HeadcountUpdateStatus.APPLIED;
    this.processedAt = Instant.now();
    this.processedBy = processorId;
  }

  public void markDismissed(UUID processorId) {
    this.status = HeadcountUpdateStatus.DISMISSED;
    this.processedAt = Instant.now();
    this.processedBy = processorId;
  }
}

