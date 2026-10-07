package lk.dmc.disaster.reports.entity;

import java.time.Instant;
import java.util.UUID;

/** What a reporter supplies when submitting; {@link HazardReport#submit} validates it. */
public record ReportDraft(
    UUID clientRef,
    UUID hazardTypeId,
    String category,
    String description,
    Double latitude,
    Double longitude,
    String manualLocationText,
    UUID districtId,
    Instant capturedAt) {}
