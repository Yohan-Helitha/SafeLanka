package lk.dmc.disaster.warnings.service;

import java.util.List;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.integration.VerifiedReportSummary;

/**
 * Everything the hazard detail screen shows.
 *
 * @param gauge the gauge and its last 24 hours of readings, or null when the hazard has no gauge
 * @param warnings the hazard's warnings, newest first
 */
public record HazardDetailView(
    HazardListEntry summary,
    List<VerifiedReportSummary> evidence,
    GaugeHistory gauge,
    List<Warning> warnings) {

  public HazardDetailView {
    evidence = List.copyOf(evidence);
    warnings = List.copyOf(warnings);
  }
}
