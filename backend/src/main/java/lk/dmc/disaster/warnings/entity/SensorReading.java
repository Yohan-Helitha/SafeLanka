package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One simulated gauge reading. */
@Entity
@Table(name = "sensor_readings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SensorReading {

  @Id private UUID id;

  @Column(name = "sensor_id", nullable = false)
  private UUID sensorId;

  @Column(nullable = false)
  private BigDecimal value;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  public static SensorReading record(UUID sensorId, BigDecimal value, Instant recordedAt) {
    SensorReading reading = new SensorReading();
    reading.id = UUID.randomUUID();
    reading.sensorId = sensorId;
    reading.value = value;
    reading.recordedAt = recordedAt;
    return reading;
  }
}
