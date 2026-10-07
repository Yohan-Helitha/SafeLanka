package lk.dmc.disaster.reports.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.geo.GeoPoint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A ground report. State only changes through {@link #verify}, {@link #reject} and {@link
 * #requestInfo}, each guarded by {@link ReportStatusMachine}.
 */
@Entity
@Table(name = "hazard_reports")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HazardReport {

  @Id private UUID id;

  @Column(name = "reference_no", nullable = false, updatable = false)
  private String referenceNo;

  @Column(name = "reporter_id", nullable = false, updatable = false)
  private UUID reporterId;

  @Column(name = "hazard_type_id", nullable = false, updatable = false)
  private UUID hazardTypeId;

  @Column(nullable = false, updatable = false)
  private String category;

  @Column(nullable = false, updatable = false)
  private String description;

  @Column(updatable = false)
  private Double latitude;

  @Column(updatable = false)
  private Double longitude;

  @Column(name = "is_manual_location", nullable = false, updatable = false)
  private boolean manualLocation;

  @Column(name = "manual_location_text", updatable = false)
  private String manualLocationText;

  @Column(name = "district_id", nullable = false, updatable = false)
  private UUID districtId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportStatus status;

  @Column(name = "client_ref", nullable = false, updatable = false)
  private UUID clientRef;

  @Column(name = "captured_at", nullable = false, updatable = false)
  private Instant capturedAt;

  @Column(name = "synced_at", nullable = false, updatable = false)
  private Instant syncedAt;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "rejection_reason")
  private RejectionReason rejectionReason;

  @Column(name = "review_comment")
  private String reviewComment;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Creates a PENDING report after checking every rule the data itself must satisfy: description
   * 10-500 characters, a location inside Sri Lanka or a 5-200 character place description, and a
   * device time at most 5 minutes ahead of the server.
   *
   * @throws BusinessRuleException (422) when a rule is broken
   */
  public static HazardReport submit(
      ReportDraft draft, UUID reporterId, String referenceNo, Clock clock) {
    Instant now = clock.instant();
    if (draft.capturedAt().isAfter(now.plus(ReportRules.CLOCK_SKEW))) {
      throw new BusinessRuleException("The capture time cannot be in the future.");
    }
    HazardReport r = new HazardReport();
    r.id = UUID.randomUUID();
    r.referenceNo = referenceNo;
    r.reporterId = reporterId;
    r.hazardTypeId = draft.hazardTypeId();
    r.category = draft.category();
    r.description = trimmedWithin(
            draft.description(), "Description", ReportRules.DESCRIPTION_MIN, ReportRules.DESCRIPTION_MAX);
    r.districtId = draft.districtId();
    r.clientRef = draft.clientRef();
    r.capturedAt = draft.capturedAt();
    r.syncedAt = now;
    r.status = ReportStatus.PENDING;
    r.applyLocation(draft);
    return r;
  }

  /** The coordinates, when the reporter shared GPS (manual-location reports have none). */
  public Optional<GeoPoint> point() {
    return manualLocation ? Optional.empty() : Optional.of(new GeoPoint(latitude, longitude));
  }

  public boolean isReportedBy(UUID userId) {
    return reporterId.equals(userId);
  }

  /** Confirms the report; the comment is optional (at most 300 characters). */
  public void verify(UUID officerId, String comment, Clock clock) {
    ReportStatus next = ReportStatusMachine.transition(status, ReportStatus.VERIFIED);
    requireReviewerIsNotReporter(officerId);
    String note = optionalComment(comment);
    decide(next, officerId, note, clock);
  }

  /** Rejects the report with a reason; the comment is required when the reason is OTHER. */
  public void reject(UUID officerId, RejectionReason reason, String comment, Clock clock) {
    ReportStatus next = ReportStatusMachine.transition(status, ReportStatus.REJECTED);
    requireReviewerIsNotReporter(officerId);
    if (reason == null) {
      throw new BusinessRuleException("A rejection reason is required.");
    }
    String note = optionalComment(comment);
    if (reason == RejectionReason.OTHER && note == null) {
      throw new BusinessRuleException("Explain the rejection when the reason is Other.");
    }
    decide(next, officerId, note, clock);
    rejectionReason = reason;
  }

  /** Asks the reporter for more detail; the comment (5-300 characters) says what is missing. */
  public void requestInfo(UUID officerId, String comment, Clock clock) {
    ReportStatus next = ReportStatusMachine.transition(status, ReportStatus.NEEDS_MORE_INFO);
    requireReviewerIsNotReporter(officerId);
    String note =
        trimmedWithin(comment, "Comment", ReportRules.REQUEST_INFO_COMMENT_MIN, ReportRules.COMMENT_MAX);
    decide(next, officerId, note, clock);
  }

  private void decide(ReportStatus next, UUID officerId, String note, Clock clock) {
    status = next;
    reviewedBy = officerId;
    reviewedAt = clock.instant();
    reviewComment = note;
  }

  private void requireReviewerIsNotReporter(UUID officerId) {
    if (isReportedBy(officerId)) {
      throw new BusinessRuleException("You cannot review your own report.");
    }
  }

  private void applyLocation(ReportDraft draft) {
    if (draft.latitude() != null && draft.longitude() != null) {
      GeoPoint p = GeoPoint.of(draft.latitude(), draft.longitude());
      latitude = p.latitude();
      longitude = p.longitude();
      manualLocation = false;
      return;
    }
    manualLocation = true;
    manualLocationText =
        trimmedWithin(
            draft.manualLocationText(),
            "Place description",
            ReportRules.MANUAL_LOCATION_MIN,
            ReportRules.MANUAL_LOCATION_MAX);
  }

  private static String optionalComment(String comment) {
    if (comment == null || comment.isBlank()) {
      return null;
    }
    return trimmedWithin(comment, "Comment", 1, ReportRules.COMMENT_MAX);
  }

  private static String trimmedWithin(String value, String label, int min, int max) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.length() < min || trimmed.length() > max) {
      throw new BusinessRuleException(label + " must be " + min + " to " + max + " characters.");
    }
    return trimmed;
  }
}
