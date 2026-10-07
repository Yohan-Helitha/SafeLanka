package lk.dmc.disaster.reports;

import java.time.Instant;
import java.util.UUID;

/** A verified report as the warnings module sees it. {@code photoUrl} is null without a photo. */
public record VerifiedReportSummary(
    UUID reportId,
    String referenceNo,
    UUID hazardTypeId,
    String category,
    String description,
    UUID districtId,
    Double latitude,
    Double longitude,
    boolean manualLocation,
    String photoUrl,
    Instant capturedAt,
    Instant verifiedAt,
    UUID verifiedBy) {}
