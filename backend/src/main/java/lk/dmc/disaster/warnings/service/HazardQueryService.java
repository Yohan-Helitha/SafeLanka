package lk.dmc.disaster.warnings.service;

import java.time.Clock;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.warnings.entity.Hazard;
import lk.dmc.disaster.warnings.entity.HazardEvidence;
import lk.dmc.disaster.warnings.entity.HazardStatus;
import lk.dmc.disaster.warnings.entity.WarningRules;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;
import lk.dmc.disaster.warnings.integration.VerifiedReports;
import lk.dmc.disaster.warnings.repository.HazardEvidenceCount;
import lk.dmc.disaster.warnings.repository.HazardEvidenceRepository;
import lk.dmc.disaster.warnings.repository.HazardRepository;
import lk.dmc.disaster.warnings.repository.HazardSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The read side of hazard assessment: the hazard list and the hazard detail. */
@Service
public class HazardQueryService {

  /** Hazards still needing attention. The list shows these unless the officer asks otherwise. */
  static final Set<HazardStatus> OPEN_STATUSES =
      EnumSet.of(HazardStatus.UNDER_ASSESSMENT, HazardStatus.WARNED, HazardStatus.MONITORING);

  private static final Sort MOST_SEVERE_FIRST =
      Sort.by(Sort.Direction.DESC, "severity", "detectedAt");

  private final HazardRepository hazards;
  private final HazardEvidenceRepository evidence;
  private final WarningQueryService warnings;
  private final GaugeReadings gauges;
  private final VerifiedReports verifiedReports;
  private final Clock clock;

  public HazardQueryService(
      HazardRepository hazards,
      HazardEvidenceRepository evidence,
      WarningQueryService warnings,
      GaugeReadings gauges,
      VerifiedReports verifiedReports,
      Clock clock) {
    this.hazards = hazards;
    this.evidence = evidence;
    this.warnings = warnings;
    this.gauges = gauges;
    this.verifiedReports = verifiedReports;
    this.clock = clock;
  }

  /**
   * Hazards, most severe first, with evidence counts and the latest gauge reading.
   *
   * @param statuses statuses to show; null or empty means the open ones
   * @param hazardTypeId only this type, or null for all
   * @param districtId only this district, or null for all
   */
  @Transactional(readOnly = true)
  public List<HazardListEntry> list(
      Collection<HazardStatus> statuses, UUID hazardTypeId, UUID districtId) {
    Collection<HazardStatus> shown =
        statuses == null || statuses.isEmpty() ? OPEN_STATUSES : statuses;
    List<Hazard> found =
        hazards.findAll(
            HazardSpecifications.statusIn(shown)
                .and(HazardSpecifications.ofType(hazardTypeId))
                .and(HazardSpecifications.inDistrict(districtId)),
            MOST_SEVERE_FIRST);
    Map<UUID, Long> counts = evidenceCounts(found);
    return found.stream()
        .map(h -> new HazardListEntry(h, counts.getOrDefault(h.getId(), 0L), latestReadingOf(h)))
        .toList();
  }

  /**
   * One hazard with its verified reports, 24 hours of gauge readings and its warnings.
   *
   * @throws AppException NOT_FOUND for an unknown hazard
   */
  @Transactional(readOnly = true)
  public HazardDetailView detail(UUID hazardId) {
    Hazard hazard =
        hazards
            .findById(hazardId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Hazard not found."));
    List<UUID> reportIds =
        evidence.findByIdHazardId(hazardId).stream().map(HazardEvidence::reportId).toList();
    List<VerifiedReportSummary> reports = verifiedReports.findVerified(reportIds);
    Optional<GaugeHistory> gauge = gaugeHistoryOf(hazard);
    GaugeReading latest = gauge.flatMap(GaugeHistory::latest).orElse(null);
    return new HazardDetailView(
        new HazardListEntry(hazard, reports.size(), latest),
        reports,
        gauge.orElse(null),
        warnings.listForHazard(hazardId));
  }

  private Map<UUID, Long> evidenceCounts(List<Hazard> found) {
    if (found.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Long> counts = new HashMap<>();
    for (HazardEvidenceCount row :
        evidence.countByHazardIds(found.stream().map(Hazard::getId).toList())) {
      counts.put(row.hazardId(), row.count());
    }
    return counts;
  }

  private GaugeReading latestReadingOf(Hazard hazard) {
    if (hazard.getSensorId() == null) {
      return null;
    }
    return gauges.latest(hazard.getSensorId()).orElse(null);
  }

  private Optional<GaugeHistory> gaugeHistoryOf(Hazard hazard) {
    if (hazard.getSensorId() == null) {
      return Optional.empty();
    }
    return gauges.history(hazard.getSensorId(), clock.instant().minus(WarningRules.CHART_WINDOW));
  }
}
