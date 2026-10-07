package lk.dmc.disaster.warnings.service;

import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;

/** The newest reading of a gauge, with the gauge so the alert levels can be compared. */
public record GaugeReading(Sensor sensor, SensorReading reading) {

  /** True when the reading is at or above the gauge alert level. */
  public boolean aboveAlert() {
    return sensor.isAtOrAboveAlert(reading.getValue());
  }
}
