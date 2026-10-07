package lk.dmc.disaster.reports;

import java.time.Instant;
import java.util.UUID;

/**
 * Published inside the verify transaction. The warnings module links the report to a hazard.
 * Coordinates are null for a report with a described place and no GPS.
 */
public record ReportVerifiedEvent(
    UUID reportId,
    UUID hazardTypeId,
    String category,
    UUID districtId,
    Double latitude,
    Double longitude,
    Instant verifiedAt) {}
