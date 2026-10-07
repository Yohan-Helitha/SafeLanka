package lk.dmc.disaster.reports.service;

import java.time.Instant;
import java.util.UUID;

/** Everything a reporter sends to submit a report. {@code photo} may be null. */
public record SubmitReportCommand(
    UUID clientRef,
    UUID hazardTypeId,
    String category,
    String description,
    Double latitude,
    Double longitude,
    String manualLocationText,
    UUID districtId,
    Instant capturedAt,
    PhotoUpload photo) {}
