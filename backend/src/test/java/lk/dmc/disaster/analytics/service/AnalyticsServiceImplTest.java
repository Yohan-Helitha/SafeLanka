package lk.dmc.disaster.analytics.service;

import static lk.dmc.disaster.analytics.AnalyticsFixtures.AUTHOR_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_END;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_START;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.KALUTARA;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.RATNAPURA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.analytics.AnalyticsFixtures;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.analytics.entity.EventSummary;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.EventSummaryQuery;
import lk.dmc.disaster.analytics.repository.DisasterReportRepository;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

  private static final Instant NOW = Instant.parse("2026-10-04T08:00:00Z");

  @Mock ReportBuilder reportBuilder;
  @Mock DisasterReportRepository repository;
  @Mock EventSummaryQuery eventSummaryQuery;
  @Mock ReferenceData referenceData;

  private AnalyticsServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new AnalyticsServiceImpl(
            reportBuilder,
            repository,
            eventSummaryQuery,
            referenceData,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static ReportContext request(Set<UUID> districts, Instant from, Instant to) {
    return new ReportContext(EVENT_ID, districts, from, to, AUTHOR_ID);
  }

  private ReportContext generateAndCaptureContext(ReportContext requested) {
    DisasterReport report = AnalyticsFixtures.emptyReport();
    when(reportBuilder.build(any())).thenReturn(report);
    when(repository.save(report)).thenReturn(report);

    service.generateReport(requested);

    ArgumentCaptor<ReportContext> built = ArgumentCaptor.forClass(ReportContext.class);
    verify(reportBuilder).build(built.capture());
    return built.getValue();
  }

  // ---- generate: defaults ------------------------------------------------------------------

  @Test
  void generateReport_withoutFilters_usesAllEventDistrictsAndTheEventWindow() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    ReportContext used = generateAndCaptureContext(request(null, null, null));

    assertThat(used.districtIds()).containsExactlyInAnyOrder(RATNAPURA, KALUTARA);
    assertThat(used.fromTime()).isEqualTo(EVENT_START);
    assertThat(used.toTime()).isEqualTo(EVENT_END);
    assertThat(used.generatedBy()).isEqualTo(AUTHOR_ID);
  }

  @Test
  void generateReport_emptyDistrictSetMeansAllDistricts() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    ReportContext used = generateAndCaptureContext(request(Set.of(), null, null));

    assertThat(used.districtIds()).containsExactlyInAnyOrder(RATNAPURA, KALUTARA);
  }

  @Test
  void generateReport_forAnActiveEvent_endsTheWindowNow() {
    Instant started = NOW.minus(Duration.ofDays(8));
    var active = AnalyticsFixtures.activeEvent(started);
    when(referenceData.event(EVENT_ID)).thenReturn(active);

    ReportContext used = generateAndCaptureContext(request(null, null, null));

    assertThat(used.fromTime()).isEqualTo(started);
    assertThat(used.toTime()).isEqualTo(NOW);
  }

  @Test
  void generateReport_narrowedFiltersArePassedThrough() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());
    Instant from = EVENT_START.plus(Duration.ofHours(5));
    Instant to = EVENT_START.plus(Duration.ofHours(20));

    ReportContext used = generateAndCaptureContext(request(Set.of(RATNAPURA), from, to));

    assertThat(used.districtIds()).containsExactly(RATNAPURA);
    assertThat(used.fromTime()).isEqualTo(from);
    assertThat(used.toTime()).isEqualTo(to);
  }

  @Test
  void generateReport_savesTheBuiltReportAndReturnsIt() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());
    DisasterReport built = AnalyticsFixtures.emptyReport();
    DisasterReport saved = AnalyticsFixtures.emptyReport();
    when(reportBuilder.build(any())).thenReturn(built);
    when(repository.save(built)).thenReturn(saved);

    assertThat(service.generateReport(request(null, null, null))).isSameAs(saved);
  }

  // ---- generate: edges ---------------------------------------------------------------------

  @Test
  void generateReport_windowMayStartAndEndWithinTheTolerance() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    ReportContext used =
        generateAndCaptureContext(
            request(null, EVENT_START.minusSeconds(30), EVENT_END.plusSeconds(30)));

    assertThat(used.fromTime()).isEqualTo(EVENT_START.minusSeconds(30));
  }

  @Test
  void generateReport_fromEqualToToIsAllowed() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());
    Instant moment = EVENT_START.plus(Duration.ofHours(3));

    ReportContext used = generateAndCaptureContext(request(null, moment, moment));

    assertThat(used.fromTime()).isEqualTo(used.toTime());
  }

  // ---- generate: rejected requests (nothing is saved) --------------------------------------

  @Test
  void generateReport_unknownEvent_isNotFoundAndSavesNothing() {
    when(referenceData.event(EVENT_ID)).thenThrow(new NotFoundException("Event not found"));

    assertThatThrownBy(() -> service.generateReport(request(null, null, null)))
        .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(reportBuilder);
    verify(repository, never()).save(any());
  }

  @Test
  void generateReport_districtOutsideTheEvent_isRejected() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    assertThatThrownBy(
            () -> service.generateReport(request(Set.of(TestIds.district(1)), null, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("districts");

    verify(repository, never()).save(any());
  }

  @Test
  void generateReport_oneGoodAndOneForeignDistrict_isRejected() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    assertThatThrownBy(
            () ->
                service.generateReport(
                    request(Set.of(RATNAPURA, TestIds.district(1)), null, null)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void generateReport_startAfterEnd_isRejected() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());
    Instant later = EVENT_START.plus(Duration.ofHours(10));

    assertThatThrownBy(
            () -> service.generateReport(request(null, later, later.minusSeconds(1))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("start");

    verify(repository, never()).save(any());
  }

  @Test
  void generateReport_windowStartingBeforeTheEvent_isRejected() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    assertThatThrownBy(
            () ->
                service.generateReport(
                    request(null, EVENT_START.minus(Duration.ofMinutes(5)), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("inside the event");
  }

  @Test
  void generateReport_windowEndingAfterTheEvent_isRejected() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());

    assertThatThrownBy(
            () ->
                service.generateReport(request(null, null, EVENT_END.plus(Duration.ofHours(1)))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("inside the event");
  }

  @Test
  void generateReport_windowEndingInTheFutureOfAnActiveEvent_isRejected() {
    when(referenceData.event(EVENT_ID))
        .thenReturn(AnalyticsFixtures.activeEvent(NOW.minus(Duration.ofDays(2))));

    assertThatThrownBy(
            () -> service.generateReport(request(null, null, NOW.plus(Duration.ofHours(2)))))
        .isInstanceOf(BusinessRuleException.class);
  }

  // ---- reading -----------------------------------------------------------------------------

  @Test
  void listAvailableEvents_asksTheEventQuery() {
    var summary =
        new EventSummary(
            EVENT_ID, "Event", TestIds.hazardType(1), "ACTIVE", NOW, null, new UUID[0], 10, 5);
    when(eventSummaryQuery.execute("ACTIVE")).thenReturn(List.of(summary));

    assertThat(service.listAvailableEvents("ACTIVE")).containsExactly(summary);
  }

  @Test
  void listSavedReports_forAnEvent_isNewestFirstFromTheRepository() {
    DisasterReport report = AnalyticsFixtures.emptyReport();
    when(repository.findByEventIdOrderByGeneratedAtDesc(EVENT_ID)).thenReturn(List.of(report));

    assertThat(service.listSavedReports(EVENT_ID)).containsExactly(report);
  }

  @Test
  void listSavedReports_withoutAnEvent_returnsAllNewestFirst() {
    when(repository.findAllByOrderByGeneratedAtDesc()).thenReturn(List.of());

    assertThat(service.listSavedReports(null)).isEmpty();
    verify(repository).findAllByOrderByGeneratedAtDesc();
  }

  @Test
  void getReport_returnsTheSavedReport() {
    UUID id = UUID.randomUUID();
    DisasterReport report = AnalyticsFixtures.emptyReport();
    when(repository.findById(id)).thenReturn(Optional.of(report));

    assertThat(service.getReport(id)).isSameAs(report);
  }

  @Test
  void getReport_unknownId_isNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getReport(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not found");
  }
}
