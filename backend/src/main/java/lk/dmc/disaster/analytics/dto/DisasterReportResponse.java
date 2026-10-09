package lk.dmc.disaster.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A saved report. {@code sections} holds the data of each available section under its camelCase
 * key ({@code alertTimeline}, {@code citizensReached}, {@code shelterOccupancy}, {@code
 * resourceDistribution}); a section without data is absent there and listed, with its reason, in
 * {@code unavailableSections}.
 */
@Schema(description = "Full disaster report data")
public record DisasterReportResponse(
    UUID id,
    UUID eventId,
    String eventName,
    @Schema(description = "districtIds, from and to; null where the report was not narrowed")
        Map<String, Object> filters,
    Map<String, Object> sections,
    @Schema(description = "[{key: SHELTER_OCCUPANCY, reason: ...}]")
        List<Map<String, String>> unavailableSections,
    GeneratedBy generatedBy,
    Instant generatedAt) {

  /** Who generated the report. */
  public record GeneratedBy(UUID id, String fullName) {}
}
