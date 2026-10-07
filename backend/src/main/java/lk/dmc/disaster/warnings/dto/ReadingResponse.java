package lk.dmc.disaster.warnings.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lk.dmc.disaster.warnings.entity.SensorReading;

/** One gauge reading. */
public record ReadingResponse(BigDecimal value, Instant recordedAt) {

  /** The reading as a response, or null when there is no reading. */
  public static ReadingResponse from(SensorReading reading) {
    return reading == null
        ? null
        : new ReadingResponse(reading.getValue(), reading.getRecordedAt());
  }
}
