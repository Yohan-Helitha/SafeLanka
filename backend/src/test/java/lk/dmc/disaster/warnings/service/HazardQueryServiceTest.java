package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.WarningLevel;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardEvidence;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.Sensor;
import lk.dmc.disaster.warnings.entity.SensorReading;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningRules;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;
import lk.dmc.disaster.warnings.integration.VerifiedReports;
import lk.dmc.disaster.warnings.repository.HazardEvidenceCount;
import lk.dmc.disaster.warnings.repository.HazardEvidenceRepository;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class HazardQueryServiceTest {

  private static final Instant NOW = ServiceFixtures.NOW;
  private static final UUID DISTRICT = UUID.randomUUID();

  @Mock private HazardRepository hazards;
  @Mock private HazardEvidenceRepository evidence;
  @Mock private WarningQueryService warnings;
  @Mock private GaugeReadings gauges;
  @Mock private VerifiedReports verifiedReports;

  private HazardQueryService service;

  @BeforeEach
  void setUp() {
    service =
        new HazardQueryService(
            hazards, evidence, warnings, gauges, verifiedReports, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @SuppressWarnings("unchecked")
  private void hazardsFound(Hazard... found) {
    when(hazards.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(found));
  }

  private static WarningView view(Warning warning) {
    return new WarningView(
        warning,
        warning.target(),
        Set.of(),
        Set.of(DISTRICT),
        new DeliveryOutcome(0, 0, 0, List.of()));
  }

  // ---- list --------------------------------------------------------------------------------

  @Test
  void list_explicitStatuses_areUsedInPlaceOfTheOpenOnes() {
    hazardsFound();

    assertThat(service.list(List.of(HazardStatus.RESOLVED), null, null)).isEmpty();
  }

  @Test
  void list_emptyStatusList_fallsBackToTheOpenStatusesLikeNull() {
    hazardsFound();

    assertThat(service.list(List.of(), null, null)).isEmpty();
    assertThat(service.list(null, null, null)).isEmpty();

    verify(hazards, org.mockito.Mockito.times(2)).findAll(any(Specification.class), any(Sort.class));
  }

  @Test
  void list_sensorHazardWhoseGaugeHasNoReadingsYet_hasNoLatestReading() {
    Sensor sensor = ServiceFixtures.sensor();
    Hazard hazard = Hazard.fromSensor(sensor, UUID.randomUUID(), "Gauge crossed alert level.", NOW);
    hazardsFound(hazard);
    when(evidence.countByHazardIds(List.of(hazard.getId()))).thenReturn(List.of());
    when(gauges.latest(sensor.getId())).thenReturn(Optional.empty());

    assertThat(service.list(null, null, null).get(0).latestReading()).isNull();
  }

  @Test
  void detail_sensorHazardWhoseGaugeIsGone_hasNoGauge() {
    Sensor sensor = ServiceFixtures.sensor();
    Hazard hazard = Hazard.fromSensor(sensor, UUID.randomUUID(), "Gauge crossed alert level.", NOW);
    when(hazards.findById(hazard.getId())).thenReturn(Optional.of(hazard));
    when(evidence.findByIdHazardId(hazard.getId())).thenReturn(List.of());
    when(verifiedReports.findVerified(List.of())).thenReturn(List.of());
    when(warnings.listForHazard(hazard.getId())).thenReturn(List.of());
    when(gauges.history(sensor.getId(), NOW.minus(WarningRules.CHART_WINDOW)))
        .thenReturn(Optional.empty());

    HazardDetailView view = service.detail(hazard.getId());

    assertThat(view.gauge()).isNull();
    assertThat(view.summary().latestReading()).isNull();
  }

  @Test
  void list_returnsEntriesWithEvidenceCountsAndZeroWhenNone() {
    Hazard withEvidence = ServiceFixtures.manualHazard(DISTRICT);
    Hazard without = ServiceFixtures.manualHazard(DISTRICT);
    hazardsFound(withEvidence, without);
    when(evidence.countByHazardIds(List.of(withEvidence.getId(), without.getId())))
        .thenReturn(List.of(new HazardEvidenceCount(withEvidence.getId(), 2)));

    List<HazardListEntry> entries = service.list(null, null, null);

    assertThat(entries).extracting(HazardListEntry::verifiedReportCount).containsExactly(2L, 0L);
    assertThat(entries).extracting(HazardListEntry::latestReading).containsOnlyNulls();
  }

  @Test
  void list_sortsMostSevereFirst() {
    hazardsFound();

    service.list(null, null, null);

    ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
    verify(hazards).findAll(any(Specification.class), sort.capture());
    assertThat(sort.getValue().stream().map(Sort.Order::getProperty))
        .containsExactly("severity", "detectedAt");
    assertThat(sort.getValue().stream().map(Sort.Order::isDescending)).containsOnly(true);
  }

  @Test
  void list_noHazards_doesNotQueryEvidence() {
    hazardsFound();

    assertThat(service.list(null, null, null)).isEmpty();

    verify(evidence, never()).countByHazardIds(any());
  }

  @Test
  void list_sensorHazard_carriesTheLatestGaugeReading() {
    Sensor sensor = ServiceFixtures.sensor();
    Hazard hazard = Hazard.fromSensor(sensor, UUID.randomUUID(), "Gauge crossed alert level.", NOW);
    SensorReading reading = ServiceFixtures.reading(sensor, "1.74", NOW);
    hazardsFound(hazard);
    when(evidence.countByHazardIds(List.of(hazard.getId()))).thenReturn(List.of());
    when(gauges.latest(sensor.getId())).thenReturn(Optional.of(new GaugeReading(sensor, reading)));

    HazardListEntry entry = service.list(null, null, null).get(0);

    assertThat(entry.latestReading().reading()).isSameAs(reading);
    assertThat(entry.latestReading().aboveAlert()).isTrue();
  }

  @Test
  void list_defaultStatusesAreTheOpenOnes() {
    assertThat(HazardQueryService.OPEN_STATUSES)
        .extracting(Enum::name)
        .containsExactlyInAnyOrder("UNDER_ASSESSMENT", "WARNED", "MONITORING");
  }

  // ---- detail ------------------------------------------------------------------------------

  @Test
  void detail_manualHazard_hasEvidenceAndWarningsButNoGauge() {
    Hazard hazard = ServiceFixtures.manualHazard(DISTRICT);
    UUID report = UUID.randomUUID();
    VerifiedReportSummary summary =
        new VerifiedReportSummary(report, "RPT-1", "FLOOD", "Water over the road.", DISTRICT, NOW);
    Warning warning =
        ServiceFixtures.warning(WarningLevel.WATCH, ServiceFixtures.districtTarget(DISTRICT), NOW);
    when(hazards.findById(hazard.getId())).thenReturn(Optional.of(hazard));
    when(evidence.findByIdHazardId(hazard.getId()))
        .thenReturn(List.of(HazardEvidence.link(hazard.getId(), report, NOW)));
    when(verifiedReports.findVerified(List.of(report))).thenReturn(List.of(summary));
    WarningView warningView = view(warning);
    when(warnings.listForHazard(hazard.getId())).thenReturn(List.of(warningView));

    HazardDetailView view = service.detail(hazard.getId());

    assertThat(view.evidence()).containsExactly(summary);
    assertThat(view.summary().verifiedReportCount()).isEqualTo(1);
    assertThat(view.warnings()).containsExactly(warningView);
    assertThat(view.gauge()).isNull();
    assertThat(view.summary().latestReading()).isNull();
  }

  @Test
  void detail_sensorHazard_includesTwentyFourHoursOfReadings() {
    Sensor sensor = ServiceFixtures.sensor();
    Hazard hazard = Hazard.fromSensor(sensor, UUID.randomUUID(), "Gauge crossed alert level.", NOW);
    SensorReading older = ServiceFixtures.reading(sensor, "1.10", NOW.minusSeconds(3600));
    SensorReading newest = ServiceFixtures.reading(sensor, "1.40", NOW);
    when(hazards.findById(hazard.getId())).thenReturn(Optional.of(hazard));
    when(evidence.findByIdHazardId(hazard.getId())).thenReturn(List.of());
    when(verifiedReports.findVerified(List.of())).thenReturn(List.of());
    when(warnings.listForHazard(hazard.getId())).thenReturn(List.of());
    when(gauges.history(sensor.getId(), NOW.minus(WarningRules.CHART_WINDOW)))
        .thenReturn(Optional.of(new GaugeHistory(sensor, List.of(older, newest))));

    HazardDetailView view = service.detail(hazard.getId());

    assertThat(view.gauge().readings()).containsExactly(older, newest);
    assertThat(view.summary().latestReading().reading()).isSameAs(newest);
  }

  @Test
  void detail_unknownHazard_isNotFound() {
    UUID id = UUID.randomUUID();
    when(hazards.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detail(id))
        .isInstanceOfSatisfying(
            AppException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test
  void gaugeHistory_emptyReadings_hasNoLatest() {
    assertThat(new GaugeHistory(ServiceFixtures.sensor(), List.of()).latest()).isEmpty();
    assertThat(Set.of(new GaugeHistory(ServiceFixtures.sensor(), List.of()))).hasSize(1);
  }
}
