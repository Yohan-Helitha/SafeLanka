package lk.dmc.disaster.warnings.dto;

import java.util.UUID;
import lk.dmc.disaster.warnings.service.TickResult;

/** Result of a simulated reading. {@code hazardId} is set only when the reading opened a hazard. */
public record TickResponse(ReadingResponse reading, boolean thresholdCrossed, UUID hazardId) {

  /** Builds the response from the result of a simulated reading. */
  public static TickResponse from(TickResult result) {
    return new TickResponse(
        ReadingResponse.from(result.reading()), result.thresholdCrossed(), result.hazardId());
  }
}
