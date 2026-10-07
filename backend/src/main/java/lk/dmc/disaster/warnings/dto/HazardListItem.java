package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.HazardSource;
import lk.dmc.disaster.warnings.entity.HazardStatus;

/**
 * One row of the hazard list.
 *
 * @param latestReading the newest gauge reading, or null when the hazard has no gauge
 */
public record HazardListItem(
    UUID id,
    UUID hazardTypeId,
    String hazardTypeCode,
    int severity,
    UUID districtId,
    UUID riverBasinId,
    String description,
    HazardSource source,
    HazardStatus status,
    Instant detectedAt,
    long verifiedReportCount,
    LatestReadingResponse latestReading) {}
