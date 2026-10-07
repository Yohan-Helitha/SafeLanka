package lk.dmc.disaster.warnings.service;

import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;

/**
 * A gauge with its newest reading.
 *
 * @param latestReading the newest reading, or null when the gauge has none yet
 */
public record SensorSnapshot(Sensor sensor, SensorReading latestReading) {}
