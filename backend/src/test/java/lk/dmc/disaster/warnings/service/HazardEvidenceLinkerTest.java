package lk.dmc.disaster.warnings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardEvidence;
import lk.dmc.disaster.warnings.entity.HazardSource;
import lk.dmc.disaster.warnings.entity.WarningRules;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.repository.HazardEvidenceRepository;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HazardEvidenceLinkerTest {

  private static final UUID TYPE = UUID.randomUUID();
  private static final UUID DISTRICT = UUID.randomUUID();
  private static final UUID BASIN = UUID.randomUUID();

  @Mock private HazardEvidenceRepository evidence;
  @Mock private HazardRepository hazards;
  @Mock private AreaReference areas;

  private HazardEvidenceLinker linker;
  private VerifiedReportRef report;

  @BeforeEach
  void setUp() {
    linker =
        new HazardEvidenceLinker(
            evidence, hazards, areas, Clock.fixed(ServiceFixtures.NOW, ZoneOffset.UTC));
    report =
        new VerifiedReportRef(
            UUID.randomUUID(), TYPE, DISTRICT, "Water over the road near the bridge.");
  }

  @Test
  void link_matchingOpenHazard_isUsedAndNoNewHazardCreated() {
    Hazard existing = ServiceFixtures.manualHazard(DISTRICT);
    when(evidence.existsByIdReportId(report.reportId())).thenReturn(false);
    when(areas.basinsOfDistrict(DISTRICT)).thenReturn(Set.of(BASIN));
    when(hazards.findNewestOpenMatching(TYPE, DISTRICT, Set.of(BASIN)))
        .thenReturn(Optional.of(existing));

    Optional<Hazard> result = linker.link(report);

    assertThat(result).containsSame(existing);
    verify(hazards, never()).save(any());
    ArgumentCaptor<HazardEvidence> saved = ArgumentCaptor.forClass(HazardEvidence.class);
    verify(evidence).save(saved.capture());
    assertThat(saved.getValue().hazardId()).isEqualTo(existing.getId());
    assertThat(saved.getValue().reportId()).isEqualTo(report.reportId());
    assertThat(saved.getValue().getLinkedAt()).isEqualTo(ServiceFixtures.NOW);
  }

  @Test
  void link_noMatchingHazard_createsAReportHazardAndLinksIt() {
    when(evidence.existsByIdReportId(report.reportId())).thenReturn(false);
    when(areas.basinsOfDistrict(DISTRICT)).thenReturn(Set.of());
    when(hazards.findNewestOpenMatching(TYPE, DISTRICT, Set.of())).thenReturn(Optional.empty());
    when(hazards.save(any(Hazard.class))).thenAnswer(call -> call.getArgument(0));

    Hazard created = linker.link(report).orElseThrow();

    assertThat(created.getSource()).isEqualTo(HazardSource.REPORT);
    assertThat(created.getSeverity()).isEqualTo(WarningRules.REPORT_HAZARD_SEVERITY);
    assertThat(created.getDistrictId()).isEqualTo(DISTRICT);
    assertThat(created.getHazardTypeId()).isEqualTo(TYPE);
    assertThat(created.getDescription()).isEqualTo(report.description());
    verify(evidence).save(any(HazardEvidence.class));
  }

  private Hazard existingReportHazard() {
    Hazard existing =
        Hazard.fromReport(
            TYPE,
            new HazardArea(DISTRICT, null),
            "Water over the road near the bridge.",
            ServiceFixtures.NOW);
    when(evidence.existsByIdReportId(report.reportId())).thenReturn(false);
    when(areas.basinsOfDistrict(DISTRICT)).thenReturn(Set.of());
    when(hazards.findNewestOpenMatching(TYPE, DISTRICT, Set.of()))
        .thenReturn(Optional.of(existing));
    return existing;
  }

  @Test
  void link_thirdReport_raisesTheHazardSeverityFromTwoToThree() {
    Hazard existing = existingReportHazard();
    when(evidence.countByIdHazardId(existing.getId())).thenReturn(3L);

    linker.link(report);

    assertThat(existing.getSeverity()).isEqualTo(3);
  }

  @Test
  void link_manyReports_reachTheTopSeverity() {
    Hazard existing = existingReportHazard();
    when(evidence.countByIdHazardId(existing.getId())).thenReturn(8L);

    linker.link(report);

    assertThat(existing.getSeverity()).isEqualTo(WarningRules.SEVERITY_MAX);
  }

  @Test
  void link_secondReport_keepsTheStartingSeverity() {
    Hazard existing = existingReportHazard();
    when(evidence.countByIdHazardId(existing.getId())).thenReturn(2L);

    linker.link(report);

    assertThat(existing.getSeverity()).isEqualTo(WarningRules.REPORT_HAZARD_SEVERITY);
  }

  @Test
  void link_neverLowersASeverityTheOfficerSetHigher() {
    Hazard existing = existingReportHazard();
    existing.setSeverity(5);
    when(evidence.countByIdHazardId(existing.getId())).thenReturn(3L);

    linker.link(report);

    assertThat(existing.getSeverity()).isEqualTo(5);
  }

  @Test
  void link_newHazard_startsAtTheSeverityTheOfficerChoseWhenVerifying() {
    when(evidence.existsByIdReportId(report.reportId())).thenReturn(false);
    when(areas.basinsOfDistrict(DISTRICT)).thenReturn(Set.of());
    when(hazards.findNewestOpenMatching(TYPE, DISTRICT, Set.of())).thenReturn(Optional.empty());
    when(hazards.save(any(Hazard.class))).thenAnswer(call -> call.getArgument(0));
    VerifiedReportRef judged =
        new VerifiedReportRef(report.reportId(), TYPE, DISTRICT, report.description(), 4);

    Hazard created = linker.link(judged).orElseThrow();

    assertThat(created.getSeverity()).isEqualTo(4);
  }

  @Test
  void link_existingHazard_isRaisedToTheOfficersSeverity() {
    Hazard existing = existingReportHazard();
    when(evidence.countByIdHazardId(existing.getId())).thenReturn(1L);
    VerifiedReportRef judged =
        new VerifiedReportRef(report.reportId(), TYPE, DISTRICT, report.description(), 5);

    linker.link(judged);

    assertThat(existing.getSeverity()).isEqualTo(5);
  }

  @Test
  void link_officersLowerSeverity_neverLowersAnExistingHazard() {
    Hazard existing = existingReportHazard();
    existing.setSeverity(4);
    when(evidence.countByIdHazardId(existing.getId())).thenReturn(1L);
    VerifiedReportRef judged =
        new VerifiedReportRef(report.reportId(), TYPE, DISTRICT, report.description(), 1);

    linker.link(judged);

    assertThat(existing.getSeverity()).isEqualTo(4);
  }

  @Test
  void link_reportAlreadyLinked_doesNothing() {
    when(evidence.existsByIdReportId(report.reportId())).thenReturn(true);

    assertThat(linker.link(report)).isEmpty();

    verify(hazards, never()).save(any());
    verify(evidence, never()).save(any());
  }

  @Test
  void link_secondIdenticalEvent_isIgnored() {
    Hazard existing = ServiceFixtures.manualHazard(DISTRICT);
    when(evidence.existsByIdReportId(report.reportId())).thenReturn(false, true);
    when(areas.basinsOfDistrict(DISTRICT)).thenReturn(Set.of());
    when(hazards.findNewestOpenMatching(TYPE, DISTRICT, Set.of()))
        .thenReturn(Optional.of(existing));

    assertThat(linker.link(report)).isPresent();
    assertThat(linker.link(report)).isEmpty();

    verify(evidence).save(any(HazardEvidence.class));
  }
}
