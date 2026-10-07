package lk.dmc.disaster.warnings.service;

import java.util.UUID;
import lk.dmc.disaster.warnings.entity.SensorReading;

/**
 * What one simulated reading did.
 *
 * @param thresholdCrossed true when this reading is the first at or above the alert level
 * @param hazardId the hazard this reading opened, or null when it opened none
 */
public record TickResult(SensorReading reading, boolean thresholdCrossed, UUID hazardId) {}
