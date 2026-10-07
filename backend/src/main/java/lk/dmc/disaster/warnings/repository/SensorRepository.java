package lk.dmc.disaster.warnings.repository;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

/** Simulated gauges (seeded master data). */
public interface SensorRepository extends JpaRepository<Sensor, UUID> {}
