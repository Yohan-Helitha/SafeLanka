package lk.dmc.disaster.reports.controller;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.entity.ReportStatus;

/** A report in a list, and the result of submitting one. Matches the frontend {@code ReportListItem}. */
public record ReportListItemResponse(
    UUID id,
    String referenceNo,
    UUID hazardTypeId,
    String category,
    String description,
    UUID districtId,
    ReportStatus status,
    Instant capturedAt,
    Instant syncedAt,
    boolean hasPhoto,
    boolean hasDuplicates,
    RejectionReason rejectionReason,
    String reviewComment) {}
