package lk.dmc.disaster.warnings.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** The newest gauge reading of a hazard, with whether it is at or above the alert level. */
public record LatestReadingResponse(
    String sensorName, BigDecimal value, String unit, boolean aboveAlert, Instant recordedAt) {}
