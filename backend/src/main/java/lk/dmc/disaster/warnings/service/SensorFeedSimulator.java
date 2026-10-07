package lk.dmc.disaster.warnings.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.entity.WarningRules;
import lk.dmc.disaster.warnings.integration.HazardTypeDirectory;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lk.dmc.disaster.warnings.repository.SensorReadingRepository;
import lk.dmc.disaster.warnings.repository.SensorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stands in for real river gauges. Each reading is either the next scripted step or a value an
 * officer sets. A gauge reaching its alert level opens a SENSOR hazard for assessment; reaching its
 * major flood level raises that hazard's severity. It never issues a warning: that is always an
 * officer's decision.
 */
@Slf4j
@Service
public class SensorFeedSimulator {

  private static final String FLOOD_CODE = "FLOOD";
  private static final BigDecimal MAX_READING = new BigDecimal("9999.99");

  private final SensorRepository sensors;
  private final SensorReadingRepository readings;
  private final HazardRepository hazards;
  private final HazardTypeDirectory hazardTypes;
  private final Clock clock;

  public SensorFeedSimulator(
      SensorRepository sensors,
      SensorReadingRepository readings,
      HazardRepository hazards,
      HazardTypeDirectory hazardTypes,
      Clock clock) {
    this.sensors = sensors;
    this.readings = readings;
    this.hazards = hazards;
    this.hazardTypes = hazardTypes;
    this.clock = clock;
  }

  /** Every gauge with its newest reading. */
  @Transactional(readOnly = true)
  public List<SensorSnapshot> listSensors() {
    return sensors.findAll().stream()
        .map(sensor -> new SensorSnapshot(sensor, latestOf(sensor).orElse(null)))
        .toList();
  }

  /**
   * Adds the next scripted reading: the newest value plus one step. A gauge with no readings starts
   * a few steps below its alert level.
   *
   * @throws AppException NOT_FOUND for an unknown gauge
   */
  @Transactional
  public TickResult tick(UUID sensorId) {
    Sensor sensor = find(sensorId);
    Optional<SensorReading> previous = latestOf(sensor);
    BigDecimal next =
        previous
            .map(reading -> reading.getValue().add(sensor.simulationStep()))
            .orElseGet(() -> startingValue(sensor));
    return record(sensor, previous, next);
  }

  /**
   * Adds a reading with the given value.
   *
   * @throws AppException VALIDATION_ERROR for a missing, negative or too large value; NOT_FOUND for
   *     an unknown gauge
   */
  @Transactional
  public TickResult setReading(UUID sensorId, BigDecimal value) {
    Sensor sensor = find(sensorId);
    BigDecimal checked = requireReasonable(value);
    return record(sensor, latestOf(sensor), checked);
  }

  private TickResult record(Sensor sensor, Optional<SensorReading> previous, BigDecimal value) {
    SensorReading reading =
        readings.save(SensorReading.record(sensor.getId(), value, clock.instant()));
    boolean crossed =
        sensor.isAtOrAboveAlert(value)
            && previous.map(p -> !sensor.isAtOrAboveAlert(p.getValue())).orElse(true);
    UUID openedHazardId = reactToThresholds(sensor, value);
    log.info(
        "Sensor {} read {}, crossed={}, hazard={}",
        sensor.getCode(),
        value,
        crossed,
        openedHazardId);
    return new TickResult(reading, crossed, openedHazardId);
  }

  /** Opens a hazard at the alert level (once) and raises its severity at major flood level. */
  private UUID reactToThresholds(Sensor sensor, BigDecimal value) {
    if (!sensor.isAtOrAboveAlert(value)) {
      return null;
    }
    Optional<Hazard> open = hazards.findOpenForSensor(sensor.getId());
    Hazard hazard = open.orElseGet(() -> hazards.save(newHazard(sensor, value)));
    if (sensor.hasReachedMajorFlood(value)) {
      hazard.raiseSeverity(WarningRules.MAJOR_FLOOD_SEVERITY);
    }
    return open.isPresent() ? null : hazard.getId();
  }

  private Hazard newHazard(Sensor sensor, BigDecimal value) {
    String description =
        "%s reached %s %s, above its alert level of %s %s."
            .formatted(
                sensor.getName(),
                value.toPlainString(),
                sensor.getUnit(),
                sensor.getAlertLevel().toPlainString(),
                sensor.getUnit());
    return Hazard.fromSensor(sensor, floodTypeId(), description, clock.instant());
  }

  private UUID floodTypeId() {
    return hazardTypes.codesById().entrySet().stream()
        .filter(entry -> FLOOD_CODE.equals(entry.getValue()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow(
            () ->
                new AppException(
                    ErrorCode.INTERNAL_ERROR, "Hazard type " + FLOOD_CODE + " is not configured."));
  }

  private static BigDecimal startingValue(Sensor sensor) {
    BigDecimal below =
        sensor
            .simulationStep()
            .multiply(BigDecimal.valueOf(WarningRules.SENSOR_START_STEPS_BELOW_ALERT));
    return sensor.getAlertLevel().subtract(below).max(BigDecimal.ZERO);
  }

  private static BigDecimal requireReasonable(BigDecimal value) {
    if (value == null
        || value.signum() < 0
        || value.setScale(2, RoundingMode.HALF_UP).compareTo(MAX_READING) > 0) {
      throw new AppException(
          ErrorCode.VALIDATION_ERROR,
          "value must be between 0 and " + MAX_READING.toPlainString() + ".",
          Map.of("field", "value"));
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private Optional<SensorReading> latestOf(Sensor sensor) {
    return readings.findFirstBySensorIdOrderByRecordedAtDesc(sensor.getId());
  }

  private Sensor find(UUID sensorId) {
    return sensors
        .findById(sensorId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Sensor not found."));
  }
}
