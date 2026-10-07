package lk.dmc.disaster.warnings.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.HazardSource;
import lk.dmc.disaster.warnings.entity.HazardStatus;

/**
 * Everything the hazard detail screen shows: the list fields plus evidence, gauge and warnings.
 *
 * @param sensor the gauge and its last 24 hours of readings, or null when the hazard has no gauge
 */
public record HazardDetail(
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
    LatestReadingResponse latestReading,
    List<EvidenceResponse> evidence,
    GaugeResponse sensor,
    List<HazardWarningRef> warnings) {}
