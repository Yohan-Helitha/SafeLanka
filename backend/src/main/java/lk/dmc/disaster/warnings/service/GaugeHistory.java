package lk.dmc.disaster.warnings.service;

import java.util.List;
import java.util.Optional;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;

/** A gauge with its recent readings, oldest first, for the hazard chart. */
public record GaugeHistory(Sensor sensor, List<SensorReading> readings) {

  public GaugeHistory {
    readings = List.copyOf(readings);
  }

  /** The newest reading, if there are any. */
  public Optional<GaugeReading> latest() {
    if (readings.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new GaugeReading(sensor, readings.get(readings.size() - 1)));
  }
}
