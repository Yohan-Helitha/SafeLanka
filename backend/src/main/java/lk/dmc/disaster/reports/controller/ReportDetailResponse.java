package lk.dmc.disaster.reports.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.entity.ReportStatus;

/**
 * One report in full. Matches the frontend {@code ReportDetail}: the list fields, the location
 * (coordinates are left out when there is no GPS), the reporter and the possible duplicates.
 */
public record ReportDetailResponse(
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
    String reviewComment,
    @JsonInclude(JsonInclude.Include.NON_NULL) Double latitude,
    @JsonInclude(JsonInclude.Include.NON_NULL) Double longitude,
    boolean isManualLocation,
    String manualLocationText,
    String photoUrl,
    Reporter reporter,
    String reviewedBy,
    Instant reviewedAt,
    List<Duplicate> possibleDuplicates,
    String reporterReply,
    Instant repliedAt) {

  /** Who submitted the report. */
  public record Reporter(UUID id, String fullName, String role) {}

  /** Another report that may describe the same event. */
  public record Duplicate(UUID id, String referenceNo, long distanceMetres) {}
}
