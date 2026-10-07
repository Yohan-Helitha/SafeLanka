package lk.dmc.disaster.warnings.service;

import java.time.Clock;
import java.util.Optional;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardArea;
import lk.dmc.disaster.warnings.entity.HazardEvidence;
import lk.dmc.disaster.warnings.integration.AreaReference;
import lk.dmc.disaster.warnings.repository.HazardEvidenceRepository;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attaches a newly verified report to the hazard it belongs to, or starts a new hazard for it. Safe
 * to run twice for the same report. {@link HazardEvidenceListener} calls it when the reports module
 * publishes a verified report.
 */
@Slf4j
@Service
public class HazardEvidenceLinker {

  private final HazardEvidenceRepository evidence;
  private final HazardRepository hazards;
  private final AreaReference areas;
  private final Clock clock;

  public HazardEvidenceLinker(
      HazardEvidenceRepository evidence,
      HazardRepository hazards,
      AreaReference areas,
      Clock clock) {
    this.evidence = evidence;
    this.hazards = hazards;
    this.areas = areas;
    this.clock = clock;
  }

  /**
   * Links the report to the newest open hazard of the same type in its district or a basin
   * containing it; with none, creates a REPORT hazard for it.
   *
   * @return the hazard the report was linked to, or empty when the report was already linked
   */
  @Transactional
  public Optional<Hazard> link(VerifiedReportRef report) {
    if (evidence.existsByIdReportId(report.reportId())) {
      log.debug("Report {} already linked, skipped", report.reportId());
      return Optional.empty();
    }
    Hazard hazard =
        hazards
            .findNewestOpenMatching(
                report.hazardTypeId(),
                report.districtId(),
                areas.basinsOfDistrict(report.districtId()))
            .orElseGet(() -> hazards.save(newHazardFor(report)));
    evidence.save(HazardEvidence.link(hazard.getId(), report.reportId(), clock.instant()));
    log.info("Report {} linked to hazard {}", report.reportId(), hazard.getId());
    return Optional.of(hazard);
  }

  private Hazard newHazardFor(VerifiedReportRef report) {
    return Hazard.fromReport(
        report.hazardTypeId(),
        new HazardArea(report.districtId(), null),
        report.description(),
        clock.instant());
  }
}
