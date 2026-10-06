package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A simulated river or rain gauge. Seeded master data; this module only reads it. */
@Entity
@Table(name = "sensors")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sensor {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SensorKind kind;

  @Column(name = "river_basin_id", nullable = false)
  private UUID riverBasinId;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  @Column(name = "alert_level", nullable = false)
  private BigDecimal alertLevel;

  @Column(name = "major_flood_level", nullable = false)
  private BigDecimal majorFloodLevel;

  @Column(nullable = false)
  private String unit;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** True when the reading is at or above the alert level. */
  public boolean isAtOrAboveAlert(BigDecimal reading) {
    return reading.compareTo(alertLevel) >= 0;
  }

  /** True when the reading is at or above the major flood level. */
  public boolean hasReachedMajorFlood(BigDecimal reading) {
    return reading.compareTo(majorFloodLevel) >= 0;
  }

  /**
   * How much one simulated tick raises the reading: an eighth of the alert-to-major gap, at least
   * 0.05.
   */
  public BigDecimal simulationStep() {
    BigDecimal step =
        majorFloodLevel
            .subtract(alertLevel)
            .divide(BigDecimal.valueOf(WarningRules.SENSOR_STEP_DIVISOR), 2, RoundingMode.HALF_UP);
    return step.max(WarningRules.SENSOR_MIN_STEP);
  }
}
