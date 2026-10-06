package lk.dmc.disaster.warnings.service;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.WarningCancelledEvent;
import lk.dmc.disaster.warnings.WarningEscalatedEvent;
import lk.dmc.disaster.warnings.WarningPublishedEvent;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The warning life cycle: publish, edit the text, escalate and cancel. Rules about the warning
 * itself live in the {@link Warning} entity; this class decides the order of the steps, sends the
 * warning and announces what happened to other modules.
 */
@Slf4j
@Service
public class WarningPublicationService {

  private final WarningRepository warnings;
  private final PublishPreconditions preconditions;
  private final AudienceService audience;
  private final NotificationDispatchService dispatch;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public WarningPublicationService(
      WarningRepository warnings,
      PublishPreconditions preconditions,
      AudienceService audience,
      NotificationDispatchService dispatch,
      ApplicationEventPublisher events,
      Clock clock) {
    this.warnings = warnings;
    this.preconditions = preconditions;
    this.audience = audience;
    this.dispatch = dispatch;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Issues a new warning, marks its hazard WARNED, sends it and announces it.
   *
   * @throws AppException BUSINESS_RULE when not confirmed, the hazard is resolved or evidence is
   *     unverified; NOT_FOUND for an unknown hazard; CONFLICT for an overlapping ACTIVE warning
   */
  @Transactional
  public Warning publish(PublishCommand command) {
    requireConfirmed(command.confirmed());
    Warning warning = Warning.publish(command.draft(), command.issuedBy(), clock.instant());
    Set<UUID> districts = resolvedDistricts(warning);
    Hazard hazard = preconditions.check(command.draft(), districts);

    hazard.markWarned();
    Warning saved = warnings.save(warning);
    dispatch.dispatch(saved);
    events.publishEvent(
        new WarningPublishedEvent(
            saved.getId(),
            saved.getLevel(),
            saved.getEventId(),
            saved.target().districtIds(),
            saved.target().riverBasinIds(),
            districts,
            saved.getIssuedAt()));
    log.info(
        "Warning {} published at level {} for hazard {}",
        saved.getId(),
        saved.getLevel(),
        saved.getHazardId());
    return saved;
  }

  /**
   * Changes the texts of an ACTIVE warning. Nothing is sent again.
   *
   * @throws AppException NOT_FOUND for an unknown warning; INVALID_STATE_TRANSITION when not ACTIVE
   */
  @Transactional
  public Warning updateContent(UUID warningId, WarningContent content) {
    Warning warning = find(warningId);
    warning.updateContent(content);
    log.info("Warning {} text updated", warningId);
    return warning;
  }

  /**
   * Replaces an ACTIVE warning with a higher-level one for the same areas and sends it again.
   *
   * @return the new warning, which supersedes the old one
   * @throws AppException BUSINESS_RULE when not confirmed or the level is not higher; NOT_FOUND;
   *     INVALID_STATE_TRANSITION when the warning is not ACTIVE
   */
  @Transactional
  public Warning escalate(EscalateCommand command) {
    requireConfirmed(command.confirmed());
    Warning old = find(command.warningId());
    WarningContent content = command.newContent() == null ? old.content() : command.newContent();

    Warning next = old.escalateTo(command.level(), content, command.issuedBy(), clock.instant());
    Warning saved = warnings.save(next);
    dispatch.dispatch(saved);
    events.publishEvent(
        new WarningEscalatedEvent(
            old.getId(),
            saved.getId(),
            old.getLevel(),
            saved.getLevel(),
            resolvedDistricts(saved),
            saved.getIssuedAt()));
    log.info("Warning {} escalated to {} as {}", old.getId(), saved.getLevel(), saved.getId());
    return saved;
  }

  /**
   * Cancels an ACTIVE warning.
   *
   * @throws AppException VALIDATION_ERROR for a missing reason; NOT_FOUND; INVALID_STATE_TRANSITION
   *     when the warning is not ACTIVE
   */
  @Transactional
  public Warning cancel(UUID warningId, String reason) {
    Warning warning = find(warningId);
    warning.cancel(reason, clock.instant());
    events.publishEvent(
        new WarningCancelledEvent(
            warning.getId(),
            warning.getCancelReason(),
            resolvedDistricts(warning),
            warning.getCancelledAt()));
    log.info("Warning {} cancelled", warningId);
    return warning;
  }

  private Warning find(UUID warningId) {
    return warnings
        .findById(warningId)
        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Warning not found."));
  }

  private Set<UUID> resolvedDistricts(Warning warning) {
    return audience.resolveDistricts(AudienceSelection.of(warning.target()));
  }

  private static void requireConfirmed(boolean confirmed) {
    if (!confirmed) {
      throw new AppException(ErrorCode.BUSINESS_RULE, "Confirm the warning after reviewing it.");
    }
  }
}
