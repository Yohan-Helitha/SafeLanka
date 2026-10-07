package lk.dmc.disaster.reports.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.ReportRules;

/**
 * The {@code report} part of the multipart submit. Coordinates may be omitted when {@code
 * manualLocationText} is given. Extra fields the phone sends (such as {@code isManualLocation}) are
 * ignored: the server decides that from the coordinates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitReportRequest(
    @NotNull UUID clientRef,
    @NotNull UUID hazardTypeId,
    @NotBlank @Size(max = 40) String category,
    @NotBlank @Size(min = ReportRules.DESCRIPTION_MIN, max = ReportRules.DESCRIPTION_MAX)
        String description,
    Double latitude,
    Double longitude,
    @Size(max = ReportRules.MANUAL_LOCATION_MAX) String manualLocationText,
    @NotNull UUID districtId,
    @NotNull Instant capturedAt) {}
