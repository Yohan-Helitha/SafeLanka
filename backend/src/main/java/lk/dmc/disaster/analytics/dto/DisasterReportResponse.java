package lk.dmc.disaster.analytics.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full disaster report data")
public record DisasterReportResponse(
    UUID id,
    UUID eventId,
    String eventName,
    Map<String, Object> filters,
    Map<String, Object> sections,
    List<Map<String, String>> unavailableSections,
    String generatedBy,
    Instant generatedAt
) {}
