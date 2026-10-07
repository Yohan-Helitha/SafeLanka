package lk.dmc.disaster.warnings.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;

/** Simulated gauge readings. */
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

  Optional<SensorReading> findFirstBySensorIdOrderByRecordedAtDesc(UUID sensorId);

  /** Readings since the given time, oldest first, for the hazard chart. */
  List<SensorReading> findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(
      UUID sensorId, Instant since);
}
