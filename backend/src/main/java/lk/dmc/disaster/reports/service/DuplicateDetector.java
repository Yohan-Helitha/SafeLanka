package lk.dmc.disaster.reports.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportRules;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.shared.geo.GeoDistance;
import lk.dmc.disaster.shared.geo.GeoPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds reports that may describe the same event: same hazard type, not rejected, both with GPS,
 * captured within 2 hours and 500 m of each other. Reports without GPS are never matched.
 */
@Component
public class DuplicateDetector {

  private final HazardReportRepository reports;

  public DuplicateDetector(HazardReportRepository reports) {
    this.reports = reports;
  }

  /** Possible duplicates of {@code subject}, nearest first. */
  @Transactional(readOnly = true)
  public List<DuplicateMatch> findDuplicates(HazardReport subject) {
    Optional<GeoPoint> origin = subject.point();
    if (origin.isEmpty()) {
      return List.of();
    }
    return reports
        .findDuplicateCandidates(
            subject.getHazardTypeId(),
            subject.getId(),
            subject.getCapturedAt().minus(ReportRules.DUPLICATE_WINDOW),
            subject.getCapturedAt().plus(ReportRules.DUPLICATE_WINDOW))
        .stream()
        .flatMap(candidate -> match(origin.get(), candidate).stream())
        .sorted(Comparator.comparingDouble(DuplicateMatch::distanceMetres))
        .toList();
  }

  private static Optional<DuplicateMatch> match(GeoPoint origin, HazardReport candidate) {
    return candidate
        .point()
        .map(p -> new DuplicateMatch(candidate, GeoDistance.metresBetween(origin, p)))
        .filter(m -> m.distanceMetres() <= ReportRules.DUPLICATE_RADIUS_METRES);
  }
}
