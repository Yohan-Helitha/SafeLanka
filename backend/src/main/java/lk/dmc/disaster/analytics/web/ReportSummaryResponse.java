package lk.dmc.disaster.analytics.web;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of a disaster report")
public record ReportSummaryResponse(
    UUID id,
    UUID eventId,
    String eventName,
    Instant generatedAt,
    String generatedByName,
    int unavailableCount
) {}
