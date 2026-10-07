package lk.dmc.disaster.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.ReportRejectedEvent;
import lk.dmc.disaster.reports.ReportVerifiedEvent;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.entity.ReportDraft;
import lk.dmc.disaster.reports.entity.ReportPhoto;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.InvalidStateTransitionException;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReportVerificationServiceTest {

  private static final Instant SUBMITTED = Instant.parse("2026-10-04T08:10:02Z");
  private static final Instant DECIDED = Instant.parse("2026-10-04T09:00:00Z");
  private static final UUID REPORTER = UUID.randomUUID();
  private static final UUID OFFICER = UUID.randomUUID();
  private static final UUID HAZARD_TYPE = UUID.randomUUID();
  private static final UUID DISTRICT = UUID.randomUUID();

  @Mock HazardReportRepository reports;
  @Mock ReportQueryService queries;
  @Mock ApplicationEventPublisher events;

  private ReportVerificationService service;
  private HazardReport report;
  private final ReportDetailView view = new ReportDetailView(null, null, null, List.of(), null);

  @BeforeEach
  void setUp() {
    service =
        new ReportVerificationService(
            reports, queries, events, Clock.fixed(DECIDED, ZoneOffset.UTC));
    report = newReport(6.9391, 79.8921, null);
    lenient().when(reports.findWithLockById(report.getId())).thenReturn(Optional.of(report));
    lenient().when(queries.viewOf(report, true)).thenReturn(view);
  }

  private static HazardReport newReport(Double lat, Double lng, String manualText) {
    return HazardReport.submit(
        new ReportDraft(
            UUID.randomUUID(),
            HAZARD_TYPE,
            "RISING_WATER",
            "Water over the road near Kolonnawa canal bridge",
            lat,
            lng,
            manualText,
            DISTRICT,
            SUBMITTED),
        REPORTER,
        "RPT-2026-0013",
        Clock.fixed(SUBMITTED, ZoneOffset.UTC));
  }

  // ---- verify -------------------------------------------------------------------------------

  @Test
  void verify_marksVerifiedSavesAndPublishesEventWithTheReportDetails() {
    service.verify(report.getId(), OFFICER, "Confirmed on site");

    assertThat(report.getStatus()).isEqualTo(ReportStatus.VERIFIED);
    assertThat(report.getReviewedBy()).isEqualTo(OFFICER);
    assertThat(report.getReviewedAt()).isEqualTo(DECIDED);
    verify(reports).save(report);
    ArgumentCaptor<ReportVerifiedEvent> event = ArgumentCaptor.forClass(ReportVerifiedEvent.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue())
        .isEqualTo(
            new ReportVerifiedEvent(
                report.getId(), HAZARD_TYPE, "RISING_WATER", DISTRICT, 6.9391, 79.8921, DECIDED));
  }

  @Test
  void verify_eventForAReportWithoutGpsHasNoCoordinates() {
    HazardReport manual = newReport(null, null, "Next to the old railway bridge");
    when(reports.findWithLockById(manual.getId())).thenReturn(Optional.of(manual));

    service.verify(manual.getId(), OFFICER, null);

    ArgumentCaptor<ReportVerifiedEvent> event = ArgumentCaptor.forClass(ReportVerifiedEvent.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().latitude()).isNull();
    assertThat(event.getValue().longitude()).isNull();
  }

  @Test
  void verify_returnsTheDetailViewForOfficers() {
    assertThat(service.verify(report.getId(), OFFICER, null)).isSameAs(view);
  }

  @Test
  void verify_alsoWorksFromNeedsMoreInfo() {
    service.requestInfo(report.getId(), OFFICER, "Which side of the bridge?");

    service.verify(report.getId(), OFFICER, "Clarified by phone");

    assertThat(report.getStatus()).isEqualTo(ReportStatus.VERIFIED);
  }

  @Test
  void verify_alreadyVerifiedIsAConflictAndPublishesNothingMore() {
    service.verify(report.getId(), OFFICER, null);

    assertThatThrownBy(() -> service.verify(report.getId(), OFFICER, null))
        .isInstanceOf(InvalidStateTransitionException.class);
    verify(events).publishEvent(any(ReportVerifiedEvent.class));
  }

  @Test
  void verify_ownReportIsRejectedWithoutSavingOrPublishing() {
    assertThatThrownBy(() -> service.verify(report.getId(), REPORTER, null))
        .isInstanceOf(BusinessRuleException.class);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    verify(reports, never()).save(any());
    verifyNoInteractions(events);
  }

  @Test
  void verify_unknownReportIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(reports.findWithLockById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.verify(unknown, OFFICER, null))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(events);
  }

  // ---- reject -------------------------------------------------------------------------------

  @Test
  void reject_storesReasonSavesAndPublishesRejectedEvent() {
    service.reject(report.getId(), OFFICER, RejectionReason.NOT_A_HAZARD, "Dry road at noon");

    assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    assertThat(report.getRejectionReason()).isEqualTo(RejectionReason.NOT_A_HAZARD);
    assertThat(report.getReviewComment()).isEqualTo("Dry road at noon");
    verify(reports).save(report);
    ArgumentCaptor<ReportRejectedEvent> event = ArgumentCaptor.forClass(ReportRejectedEvent.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue())
        .isEqualTo(new ReportRejectedEvent(report.getId(), "NOT_A_HAZARD", DECIDED));
  }

  @Test
  void reject_otherWithoutCommentIsRejectedAndNothingHappens() {
    assertThatThrownBy(() -> service.reject(report.getId(), OFFICER, RejectionReason.OTHER, " "))
        .isInstanceOf(BusinessRuleException.class);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    verify(reports, never()).save(any());
    verifyNoInteractions(events);
  }

  @Test
  void reject_withoutReasonIsRejected() {
    assertThatThrownBy(() -> service.reject(report.getId(), OFFICER, null, "No reason"))
        .isInstanceOf(BusinessRuleException.class);
    verifyNoInteractions(events);
  }

  @Test
  void reject_afterVerifiedIsAConflict() {
    service.verify(report.getId(), OFFICER, null);

    assertThatThrownBy(
            () -> service.reject(report.getId(), OFFICER, RejectionReason.DUPLICATE, null))
        .isInstanceOf(InvalidStateTransitionException.class);
    verify(events, never()).publishEvent(any(ReportRejectedEvent.class));
  }

  @Test
  void reject_ownReportIsRejectedWithBusinessRule() {
    assertThatThrownBy(
            () -> service.reject(report.getId(), REPORTER, RejectionReason.DUPLICATE, null))
        .isInstanceOf(BusinessRuleException.class);
    verifyNoInteractions(events);
  }

  @Test
  void reject_unknownReportIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(reports.findWithLockById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reject(unknown, OFFICER, RejectionReason.DUPLICATE, null))
        .isInstanceOf(NotFoundException.class);
  }

  // ---- request info -------------------------------------------------------------------------

  @Test
  void requestInfo_movesToNeedsMoreInfoSavesAndPublishesNothing() {
    service.requestInfo(report.getId(), OFFICER, "Which side of the bridge?");

    assertThat(report.getStatus()).isEqualTo(ReportStatus.NEEDS_MORE_INFO);
    assertThat(report.getReviewComment()).isEqualTo("Which side of the bridge?");
    verify(reports).save(report);
    verifyNoInteractions(events);
  }

  @Test
  void requestInfo_onlyFromPending() {
    service.requestInfo(report.getId(), OFFICER, "Which side of the bridge?");

    assertThatThrownBy(() -> service.requestInfo(report.getId(), OFFICER, "Which side again?"))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void requestInfo_afterVerifiedIsAConflict() {
    service.verify(report.getId(), OFFICER, null);

    assertThatThrownBy(() -> service.requestInfo(report.getId(), OFFICER, "Which side?"))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void requestInfo_shortCommentIsRejected() {
    assertThatThrownBy(() -> service.requestInfo(report.getId(), OFFICER, "why"))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
  }

  @Test
  void requestInfo_unknownReportIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(reports.findWithLockById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestInfo(unknown, OFFICER, "Which side?"))
        .isInstanceOf(NotFoundException.class);
  }
}
