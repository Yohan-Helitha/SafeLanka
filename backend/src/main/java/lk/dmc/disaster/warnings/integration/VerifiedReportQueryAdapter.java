package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.reports.VerifiedReportQuery;
import org.springframework.stereotype.Component;

/**
 * Adapts the reports module's public {@link VerifiedReportQuery} to the {@link VerifiedReports}
 * port, keeping only the fields the warnings module shows as evidence.
 */
@Component
class VerifiedReportQueryAdapter implements VerifiedReports {

  private final VerifiedReportQuery reports;

  VerifiedReportQueryAdapter(VerifiedReportQuery reports) {
    this.reports = reports;
  }

  @Override
  public List<VerifiedReportSummary> findVerified(Collection<UUID> reportIds) {
    return reportIds.stream()
        .distinct()
        .map(reports::findVerifiedById)
        .flatMap(Optional::stream)
        .map(VerifiedReportQueryAdapter::toEvidence)
        .toList();
  }

  private static VerifiedReportSummary toEvidence(lk.dmc.disaster.reports.VerifiedReportSummary r) {
    return new VerifiedReportSummary(
        r.reportId(),
        r.referenceNo(),
        r.category(),
        r.description(),
        r.districtId(),
        r.capturedAt());
  }
}
