package lk.dmc.disaster.warnings.integration;

import java.time.Instant;
import java.util.UUID;

/** The part of a verified report that the warnings module shows as evidence. */
public record VerifiedReportSummary(
    UUID id,
    String referenceNo,
    String category,
    String description,
    UUID districtId,
    Instant capturedAt) {}
