package lk.dmc.disaster.analytics.web;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to generate a disaster report")
public record GenerateReportRequest(
    @NotNull @Schema(description = "Event ID") UUID eventId,
    @Schema(description = "Optional set of district IDs to filter by") Set<UUID> districtIds,
    @Schema(description = "Optional start time filter") Instant from,
    @Schema(description = "Optional end time filter") Instant to
) {}
