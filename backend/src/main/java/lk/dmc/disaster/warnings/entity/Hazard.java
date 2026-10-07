package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A hazard a duty officer assesses before deciding whether to warn the public. */
@Entity
@Table(name = "hazards")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hazard {

  @Id private UUID id;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "hazard_type_id", nullable = false)
  private UUID hazardTypeId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(nullable = false)
  private int severity;

  @Column(name = "district_id")
  private UUID districtId;

  @Column(name = "river_basin_id")
  private UUID riverBasinId;

  @Column(nullable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HazardSource source;

  @Column(name = "sensor_id")
  private UUID sensorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HazardStatus status;

  @Column(name = "detected_at", nullable = false)
  private Instant detectedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** An officer records a hazard by hand. */
  public static Hazard manual(
      UUID hazardTypeId,
      int severity,
      HazardArea area,
      String description,
      UUID eventId,
      Instant now) {
    Hazard hazard = open(hazardTypeId, severity, area, description, HazardSource.MANUAL, now);
    hazard.eventId = eventId;
    return hazard;
  }

  /** A verified report with no matching hazard starts one. */
  public static Hazard fromReport(
      UUID hazardTypeId, HazardArea area, String description, Instant now) {
    return open(
        hazardTypeId,
        WarningRules.REPORT_HAZARD_SEVERITY,
        area,
        description,
        HazardSource.REPORT,
        now);
  }

  /** A simulated gauge crossing its alert level starts one. */
  public static Hazard fromSensor(
      Sensor sensor, UUID hazardTypeId, String description, Instant now) {
    HazardArea area = new HazardArea(sensor.getDistrictId(), sensor.getRiverBasinId());
    Hazard hazard =
        open(
            hazardTypeId,
            WarningRules.SENSOR_HAZARD_SEVERITY,
            area,
            description,
            HazardSource.SENSOR,
            now);
    hazard.sensorId = sensor.getId();
    return hazard;
  }

  private static Hazard open(
      UUID hazardTypeId,
      int severity,
      HazardArea area,
      String description,
      HazardSource source,
      Instant now) {
    Hazard hazard = new Hazard();
    hazard.id = UUID.randomUUID();
    hazard.hazardTypeId = hazardTypeId;
    hazard.severity = WarningRules.requireSeverity(severity);
    hazard.districtId = area.districtId();
    hazard.riverBasinId = area.riverBasinId();
    hazard.description =
        WarningRules.requireText(
            "description",
            description,
            WarningRules.HAZARD_DESCRIPTION_MIN,
            WarningRules.HAZARD_DESCRIPTION_MAX);
    hazard.source = source;
    hazard.status = HazardStatus.UNDER_ASSESSMENT;
    hazard.detectedAt = now;
    return hazard;
  }

  /**
   * Moves the hazard to a new assessment status.
   *
   * @throws lk.dmc.disaster.shared.error.AppException INVALID_STATE_TRANSITION when not allowed
   */
  public void assessAs(HazardStatus next) {
    HazardStatusMachine.require(status, next);
    status = next;
  }

  /** Records that a warning was issued. A hazard already WARNED (escalation) stays WARNED. */
  public void markWarned() {
    if (status != HazardStatus.WARNED) {
      assessAs(HazardStatus.WARNED);
    }
  }

  /**
   * Sets the severity the officer judges right, up or down.
   *
   * @throws lk.dmc.disaster.shared.error.AppException VALIDATION_ERROR outside 1 to 5;
   *     INVALID_STATE_TRANSITION when the hazard is resolved
   */
  public void setSeverity(int newSeverity) {
    WarningRules.requireSeverity(newSeverity);
    if (!isOpen()) {
      throw new AppException(
          ErrorCode.INVALID_STATE_TRANSITION, "A resolved hazard cannot be changed.");
    }
    severity = newSeverity;
  }

  /** Raises severity to at least {@code minimum}; never lowers it. */
  public void raiseSeverity(int minimum) {
    severity = Math.max(severity, WarningRules.requireSeverity(minimum));
  }

  /** A hazard stays open until it is resolved. */
  public boolean isOpen() {
    return status != HazardStatus.RESOLVED;
  }
}
