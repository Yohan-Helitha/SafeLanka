package lk.dmc.disaster.warnings.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A public warning. Every change goes through a method here so the status rules and text limits
 * cannot be bypassed.
 */
@Entity
@Table(name = "warnings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warning {

  @Id private UUID id;

  @Column(name = "hazard_id", nullable = false)
  private UUID hazardId;

  @Column(name = "event_id")
  private UUID eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WarningLevel level;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WarningStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false)
  private TargetType targetType;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String message;

  @Column(name = "sms_text", nullable = false)
  private String smsText;

  @Column(nullable = false)
  private String instructions;

  @Column(name = "issued_by", nullable = false)
  private UUID issuedBy;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "supersedes_id")
  private UUID supersedesId;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Getter(AccessLevel.NONE)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "warning_id", nullable = false)
  private List<WarningTargetArea> targetAreas = new ArrayList<>();

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @CollectionTable(name = "warning_reports", joinColumns = @JoinColumn(name = "warning_id"))
  @Column(name = "report_id")
  private Set<UUID> evidenceReportIds = new HashSet<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Issues a new ACTIVE warning from the officer's draft. */
  public static Warning publish(WarningDraft draft, UUID issuedBy, Instant now) {
    Warning warning = new Warning();
    warning.id = UUID.randomUUID();
    warning.hazardId = draft.hazardId();
    warning.eventId = draft.eventId();
    warning.level = draft.level();
    warning.status = WarningStatus.ACTIVE;
    warning.targetType = draft.target().type();
    warning.applyContent(draft.content());
    warning.issuedBy = issuedBy;
    warning.issuedAt = now;
    warning.evidenceReportIds.addAll(draft.evidenceReportIds());
    draft
        .target()
        .districtIds()
        .forEach(d -> warning.targetAreas.add(WarningTargetArea.ofDistrict(d)));
    draft
        .target()
        .riverBasinIds()
        .forEach(b -> warning.targetAreas.add(WarningTargetArea.ofRiverBasin(b)));
    return warning;
  }

  /**
   * Replaces the texts. The warning is not sent again.
   *
   * @throws AppException INVALID_STATE_TRANSITION when the warning is not ACTIVE
   */
  public void updateContent(WarningContent content) {
    requireActive();
    applyContent(content);
  }

  /**
   * Replaces this warning with a higher-level one for the same targets and evidence.
   *
   * @param content the texts for the new warning (pass {@link #content()} to keep them)
   * @return the new ACTIVE warning, which supersedes this one
   * @throws AppException INVALID_STATE_TRANSITION when not ACTIVE; BUSINESS_RULE when the level is
   *     not higher
   */
  public Warning escalateTo(
      WarningLevel newLevel, WarningContent content, UUID issuedBy, Instant now) {
    WarningStatusMachine.require(status, WarningStatus.ESCALATED);
    if (!newLevel.isHigherThan(level)) {
      throw new AppException(
          ErrorCode.BUSINESS_RULE, "New level must be higher than " + level + ".");
    }
    status = WarningStatus.ESCALATED;
    Warning next =
        publish(
            new WarningDraft(hazardId, eventId, newLevel, target(), content, evidenceReportIds),
            issuedBy,
            now);
    next.supersedesId = id;
    return next;
  }

  /**
   * Cancels the warning.
   *
   * @throws AppException VALIDATION_ERROR for a missing reason; INVALID_STATE_TRANSITION when not
   *     ACTIVE
   */
  public void cancel(String reason, Instant now) {
    String trimmed =
        WarningRules.requireText(
            "reason", reason, WarningRules.CANCEL_REASON_MIN, WarningRules.CANCEL_REASON_MAX);
    WarningStatusMachine.require(status, WarningStatus.CANCELLED);
    status = WarningStatus.CANCELLED;
    cancelReason = trimmed;
    cancelledAt = now;
  }

  public boolean isActive() {
    return status == WarningStatus.ACTIVE;
  }

  /** The current texts as one value. */
  public WarningContent content() {
    return new WarningContent(title, message, smsText, instructions);
  }

  /** The areas this warning targets, as the value used to create it. */
  public WarningTarget target() {
    return new WarningTarget(
        targetType,
        areaIds(WarningTargetArea::getDistrictId),
        areaIds(WarningTargetArea::getRiverBasinId));
  }

  public List<WarningTargetArea> targetAreas() {
    return List.copyOf(targetAreas);
  }

  public Set<UUID> evidenceReportIds() {
    return Set.copyOf(evidenceReportIds);
  }

  private Set<UUID> areaIds(Function<WarningTargetArea, UUID> getter) {
    return targetAreas.stream()
        .map(getter)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  private void applyContent(WarningContent content) {
    this.title = content.title();
    this.message = content.message();
    this.smsText = content.smsText();
    this.instructions = content.instructions();
  }

  private void requireActive() {
    if (!isActive()) {
      throw new AppException(
          ErrorCode.INVALID_STATE_TRANSITION, "Only an ACTIVE warning can be changed.");
    }
  }
}
