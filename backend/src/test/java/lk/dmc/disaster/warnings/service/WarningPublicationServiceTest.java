package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.WarningCancelledEvent;
import lk.dmc.disaster.warnings.WarningEscalatedEvent;
import lk.dmc.disaster.warnings.WarningPublishedEvent;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.TargetType;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningContent;
import lk.dmc.disaster.warnings.entity.WarningDraft;
import lk.dmc.disaster.warnings.entity.WarningStatus;
import lk.dmc.disaster.warnings.entity.WarningTarget;
import lk.dmc.disaster.warnings.repository.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WarningPublicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
  private static final UUID OFFICER = UUID.randomUUID();
  private static final UUID COLOMBO = UUID.randomUUID();
  private static final UUID GAMPAHA = UUID.randomUUID();
  private static final UUID KELANI = UUID.randomUUID();

  @Mock private WarningRepository warnings;
  @Mock private PublishPreconditions preconditions;
  @Mock private AudienceService audience;
  @Mock private NotificationDispatchService dispatch;
  @Mock private ApplicationEventPublisher events;

  private WarningPublicationService service;
  private Hazard hazard;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    service =
        new WarningPublicationService(warnings, preconditions, audience, dispatch, events, clock);
    hazard =
        Hazard.manual(
            UUID.randomUUID(),
            3,
            new HazardArea(COLOMBO, null),
            "Kelani river is rising near Hanwella.",
            null,
            NOW);
  }

  private static WarningContent content() {
    return new WarningContent(
        "Kelani flood", "Water is rising fast.", "Kelani flood: move now.", "Leave homes.");
  }

  private WarningDraft draft(WarningLevel level, WarningTarget target, Set<UUID> evidence) {
    return new WarningDraft(hazard.getId(), null, level, target, content(), evidence);
  }

  private static WarningTarget basinTarget() {
    return new WarningTarget(TargetType.RIVER_BASIN, Set.of(), Set.of(KELANI));
  }

  private static WarningTarget districtTarget() {
    return new WarningTarget(TargetType.DISTRICT, Set.of(COLOMBO), Set.of());
  }

  private void warningsSaveReturnsItsArgument() {
    when(warnings.save(any(Warning.class))).thenAnswer(call -> call.getArgument(0));
  }

  private Warning activeWarning(WarningLevel level) {
    return Warning.publish(draft(level, districtTarget(), Set.of()), OFFICER, NOW);
  }

  private Warning existing(Warning warning) {
    when(warnings.findById(warning.getId())).thenReturn(Optional.of(warning));
    return warning;
  }

  private static void assertCode(Throwable thrown, ErrorCode expected) {
    assertThat(thrown)
        .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.code()).isEqualTo(expected));
  }

  // ---- publish -----------------------------------------------------------------------------

  @Test
  void publish_validCommand_savesWarningMarksHazardWarnedDispatchesAndAnnounces() {
    UUID report = UUID.randomUUID();
    WarningDraft draft = draft(WarningLevel.WARNING, basinTarget(), Set.of(report));
    Set<UUID> resolved = Set.of(COLOMBO, GAMPAHA);
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(resolved);
    when(preconditions.check(draft, resolved)).thenReturn(hazard);
    warningsSaveReturnsItsArgument();

    Warning result = service.publish(new PublishCommand(draft, true, OFFICER));

    assertThat(result.getStatus()).isEqualTo(WarningStatus.ACTIVE);
    assertThat(result.getIssuedBy()).isEqualTo(OFFICER);
    assertThat(result.getIssuedAt()).isEqualTo(NOW);
    assertThat(result.evidenceReportIds()).containsExactly(report);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.WARNED);
    verify(warnings).save(result);
    verify(dispatch).dispatch(result);

    ArgumentCaptor<WarningPublishedEvent> captor =
        ArgumentCaptor.forClass(WarningPublishedEvent.class);
    verify(events).publishEvent(captor.capture());
    WarningPublishedEvent event = captor.getValue();
    assertThat(event.warningId()).isEqualTo(result.getId());
    assertThat(event.level()).isEqualTo(WarningLevel.WARNING);
    assertThat(event.riverBasinIds()).containsExactly(KELANI);
    assertThat(event.districtIds()).isEmpty();
    assertThat(event.resolvedDistrictIds()).containsExactlyInAnyOrder(COLOMBO, GAMPAHA);
    assertThat(event.issuedAt()).isEqualTo(NOW);
  }

  @Test
  void publish_withoutEvidence_isAccepted() {
    WarningDraft draft = draft(WarningLevel.WATCH, districtTarget(), Set.of());
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));
    when(preconditions.check(draft, Set.of(COLOMBO))).thenReturn(hazard);
    warningsSaveReturnsItsArgument();

    Warning result = service.publish(new PublishCommand(draft, true, OFFICER));

    assertThat(result.evidenceReportIds()).isEmpty();
  }

  @Test
  void publish_hazardAlreadyWarned_staysWarned() {
    hazard.markWarned();
    WarningDraft draft = draft(WarningLevel.WATCH, districtTarget(), Set.of());
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));
    when(preconditions.check(draft, Set.of(COLOMBO))).thenReturn(hazard);
    warningsSaveReturnsItsArgument();

    service.publish(new PublishCommand(draft, true, OFFICER));

    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.WARNED);
  }

  @Test
  void publish_notConfirmed_isBusinessRuleAndNothingHappens() {
    WarningDraft draft = draft(WarningLevel.WARNING, districtTarget(), Set.of());

    assertThatThrownBy(() -> service.publish(new PublishCommand(draft, false, OFFICER)))
        .satisfies(e -> assertCode(e, ErrorCode.BUSINESS_RULE));

    verifyNoInteractions(preconditions, dispatch, events);
    verify(warnings, never()).save(any());
  }

  @Test
  void publish_preconditionFails_nothingIsSavedSentOrAnnounced() {
    WarningDraft draft = draft(WarningLevel.WARNING, districtTarget(), Set.of());
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));
    when(preconditions.check(draft, Set.of(COLOMBO)))
        .thenThrow(new AppException(ErrorCode.CONFLICT, "overlap"));

    assertThatThrownBy(() -> service.publish(new PublishCommand(draft, true, OFFICER)))
        .satisfies(e -> assertCode(e, ErrorCode.CONFLICT));

    verify(warnings, never()).save(any());
    verifyNoInteractions(dispatch, events);
    assertThat(hazard.getStatus()).isEqualTo(HazardStatus.UNDER_ASSESSMENT);
  }

  // ---- update ------------------------------------------------------------------------------

  @Test
  void updateContent_activeWarning_changesTextsWithoutSendingOrAnnouncing() {
    Warning warning = existing(activeWarning(WarningLevel.WATCH));
    WarningContent edited =
        new WarningContent(
            "Updated title", "Updated message body.", "Updated SMS text here.", "Updated steps.");

    Warning result = service.updateContent(warning.getId(), edited);

    assertThat(result.content()).isEqualTo(edited);
    verifyNoInteractions(dispatch, events);
  }

  @Test
  void updateContent_unknownWarning_isNotFound() {
    UUID id = UUID.randomUUID();
    when(warnings.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateContent(id, content()))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
  }

  @Test
  void updateContent_cancelledWarning_isConflict() {
    Warning warning = existing(activeWarning(WarningLevel.WATCH));
    warning.cancel("Water level receded", NOW);

    assertThatThrownBy(() -> service.updateContent(warning.getId(), content()))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
  }

  // ---- escalate ----------------------------------------------------------------------------

  @Test
  void escalate_higherLevel_savesNewWarningSendsItAgainAndAnnounces() {
    Warning old = existing(activeWarning(WarningLevel.WATCH));
    warningsSaveReturnsItsArgument();
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));

    Warning next =
        service.escalate(
            new EscalateCommand(old.getId(), WarningLevel.EVACUATE, null, true, OFFICER));

    assertThat(old.getStatus()).isEqualTo(WarningStatus.ESCALATED);
    assertThat(next.getLevel()).isEqualTo(WarningLevel.EVACUATE);
    assertThat(next.getSupersedesId()).isEqualTo(old.getId());
    assertThat(next.content()).isEqualTo(old.content());
    verify(dispatch).dispatch(next);

    ArgumentCaptor<WarningEscalatedEvent> captor =
        ArgumentCaptor.forClass(WarningEscalatedEvent.class);
    verify(events).publishEvent(captor.capture());
    WarningEscalatedEvent event = captor.getValue();
    assertThat(event.previousWarningId()).isEqualTo(old.getId());
    assertThat(event.newWarningId()).isEqualTo(next.getId());
    assertThat(event.oldLevel()).isEqualTo(WarningLevel.WATCH);
    assertThat(event.newLevel()).isEqualTo(WarningLevel.EVACUATE);
    assertThat(event.resolvedDistrictIds()).containsExactly(COLOMBO);
  }

  @Test
  void escalate_withNewText_usesTheNewText() {
    Warning old = existing(activeWarning(WarningLevel.WATCH));
    warningsSaveReturnsItsArgument();
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));
    WarningContent urgent =
        new WarningContent(
            "Evacuate now", "Leave the area immediately.", "Evacuate Kelani banks now.", "Go now.");

    Warning next =
        service.escalate(
            new EscalateCommand(old.getId(), WarningLevel.EVACUATE, urgent, true, OFFICER));

    assertThat(next.content()).isEqualTo(urgent);
  }

  @Test
  void escalate_sameOrLowerLevel_isBusinessRuleAndNothingIsSent() {
    Warning old = existing(activeWarning(WarningLevel.WARNING));

    assertThatThrownBy(
            () ->
                service.escalate(
                    new EscalateCommand(old.getId(), WarningLevel.WARNING, null, true, OFFICER)))
        .satisfies(e -> assertCode(e, ErrorCode.BUSINESS_RULE));

    assertThat(old.isActive()).isTrue();
    verifyNoInteractions(dispatch, events);
  }

  @Test
  void escalate_nonActiveWarning_isConflict() {
    Warning old = existing(activeWarning(WarningLevel.WATCH));
    old.cancel("Issued by mistake", NOW);

    assertThatThrownBy(
            () ->
                service.escalate(
                    new EscalateCommand(old.getId(), WarningLevel.EVACUATE, null, true, OFFICER)))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void escalate_notConfirmed_isBusinessRule() {
    assertThatThrownBy(
            () ->
                service.escalate(
                    new EscalateCommand(
                        UUID.randomUUID(), WarningLevel.EVACUATE, null, false, OFFICER)))
        .satisfies(e -> assertCode(e, ErrorCode.BUSINESS_RULE));
    verifyNoInteractions(warnings, dispatch, events);
  }

  @Test
  void escalate_unknownWarning_isNotFound() {
    UUID id = UUID.randomUUID();
    when(warnings.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.escalate(
                    new EscalateCommand(id, WarningLevel.EVACUATE, null, true, OFFICER)))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
  }

  // ---- cancel ------------------------------------------------------------------------------

  @Test
  void cancel_activeWarning_marksCancelledAndAnnouncesTheReason() {
    Warning warning = existing(activeWarning(WarningLevel.WATCH));
    when(audience.resolveDistricts(any(AudienceSelection.class))).thenReturn(Set.of(COLOMBO));

    Warning result = service.cancel(warning.getId(), "Water level receded");

    assertThat(result.getStatus()).isEqualTo(WarningStatus.CANCELLED);
    ArgumentCaptor<WarningCancelledEvent> captor =
        ArgumentCaptor.forClass(WarningCancelledEvent.class);
    verify(events).publishEvent(captor.capture());
    assertThat(captor.getValue().warningId()).isEqualTo(warning.getId());
    assertThat(captor.getValue().reason()).isEqualTo("Water level receded");
    assertThat(captor.getValue().resolvedDistrictIds()).containsExactly(COLOMBO);
    assertThat(captor.getValue().cancelledAt()).isEqualTo(NOW);
    verifyNoInteractions(dispatch);
  }

  @Test
  void cancel_shortReason_isValidationErrorAndNothingIsAnnounced() {
    Warning warning = existing(activeWarning(WarningLevel.WATCH));

    assertThatThrownBy(() -> service.cancel(warning.getId(), "no"))
        .satisfies(e -> assertCode(e, ErrorCode.VALIDATION_ERROR));

    assertThat(warning.isActive()).isTrue();
    verifyNoInteractions(events);
  }

  @Test
  void cancel_alreadyCancelled_isConflict() {
    Warning warning = existing(activeWarning(WarningLevel.WATCH));
    warning.cancel("Water level receded", NOW);

    assertThatThrownBy(() -> service.cancel(warning.getId(), "Cancelling again"))
        .satisfies(e -> assertCode(e, ErrorCode.INVALID_STATE_TRANSITION));
    verifyNoInteractions(events);
  }

  @Test
  void cancel_unknownWarning_isNotFound() {
    UUID id = UUID.randomUUID();
    when(warnings.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.cancel(id, "Water level receded"))
        .satisfies(e -> assertCode(e, ErrorCode.NOT_FOUND));
  }
}
