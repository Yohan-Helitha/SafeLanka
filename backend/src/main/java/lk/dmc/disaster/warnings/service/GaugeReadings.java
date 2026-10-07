package lk.dmc.disaster.warnings.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.repository.SensorReadingRepository;
import lk.dmc.disaster.warnings.repository.SensorRepository;
import org.springframework.stereotype.Component;

/** Looks up gauges and their readings, so the hazard services do not each repeat it. */
@Component
class GaugeReadings {

  private final SensorRepository sensors;
  private final SensorReadingRepository readings;

  GaugeReadings(SensorRepository sensors, SensorReadingRepository readings) {
    this.sensors = sensors;
    this.readings = readings;
  }

  /** The newest reading of the gauge, if the gauge exists and has any. */
  Optional<GaugeReading> latest(UUID sensorId) {
    return sensors
        .findById(sensorId)
        .flatMap(
            sensor ->
                readings
                    .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                    .map(reading -> new GaugeReading(sensor, reading)));
  }

  /** The gauge with its readings since the given time, oldest first. */
  Optional<GaugeHistory> history(UUID sensorId, Instant since) {
    Optional<Sensor> sensor = sensors.findById(sensorId);
    return sensor.map(
        s ->
            new GaugeHistory(
                s, readings.findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(sensorId, since)));
  }
}
