package lk.dmc.disaster.warnings.service;

import java.util.List;
import lk.dmc.disaster.reports.ReportVerifiedEvent;
import lk.dmc.disaster.warnings.integration.VerifiedReports;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to a report being verified in the reports module by linking it to a hazard. It runs after
 * the verify transaction commits, so a failure here never undoes the officer's verification; the
 * event is kept and retried.
 */
@Slf4j
@Component
public class HazardEvidenceListener {

  private final HazardEvidenceLinker linker;
  private final VerifiedReports verifiedReports;

  public HazardEvidenceListener(HazardEvidenceLinker linker, VerifiedReports verifiedReports) {
    this.linker = linker;
    this.verifiedReports = verifiedReports;
  }

  /**
   * Links the verified report to the newest matching open hazard, or starts a new REPORT hazard.
   * Handling the same event twice changes nothing.
   */
  @ApplicationModuleListener
  void on(ReportVerifiedEvent event) {
    log.debug("Report {} verified, linking to a hazard", event.reportId());
    verifiedReports.findVerified(List.of(event.reportId())).stream()
        .findFirst()
        .ifPresentOrElse(
            report ->
                linker.link(
                    new VerifiedReportRef(
                        event.reportId(),
                        event.hazardTypeId(),
                        event.districtId(),
                        report.description())),
            () -> log.warn("Verified report {} not found, no hazard linked", event.reportId()));
  }
}
