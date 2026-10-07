package lk.dmc.disaster.reports.service;

import java.util.UUID;
import lk.dmc.disaster.reports.ReportRejectedEvent;
import lk.dmc.disaster.reports.ReportVerifiedEvent;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.repository.HazardReportRepository;
import lk.dmc.disaster.shared.error.NotFoundException;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC02 main flow steps 7-8 and alternatives A3, A4: a DMC officer decides on a report. Callers must
 * already have checked the DMC_OFFICER role. The report row is locked while it is decided, so two
 * officers cannot decide the same report at once.
 */
@Slf4j
@Service
public class ReportVerificationService {

  private final HazardReportRepository reports;
  private final ReportQueryService queries;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public ReportVerificationService(
      HazardReportRepository reports,
      ReportQueryService queries,
      ApplicationEventPublisher events,
      Clock clock) {
    this.reports = reports;
    this.queries = queries;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Verifies a report and publishes {@link ReportVerifiedEvent}.
   *
   * @throws NotFoundException (404) when the report does not exist
   * @throws lk.dmc.disaster.shared.error.InvalidStateTransitionException (409) when already decided
   * @throws lk.dmc.disaster.shared.error.BusinessRuleException (422) for the officer's own report
   */
  @Transactional
  public ReportDetailView verify(UUID reportId, UUID officerId, String comment) {
    HazardReport report = load(reportId);
    report.verify(officerId, comment, clock);
    events.publishEvent(
        new ReportVerifiedEvent(
            report.getId(),
            report.getHazardTypeId(),
            report.getCategory(),
            report.getDistrictId(),
            report.getLatitude(),
            report.getLongitude(),
            report.getReviewedAt()));
    return saved(report, "verified", officerId);
  }

  /**
   * Rejects a report with a reason and publishes {@link ReportRejectedEvent}. The comment is
   * required when the reason is OTHER.
   */
  @Transactional
  public ReportDetailView reject(
      UUID reportId, UUID officerId, RejectionReason reason, String comment) {
    HazardReport report = load(reportId);
    report.reject(officerId, reason, comment, clock);
    events.publishEvent(
        new ReportRejectedEvent(report.getId(), reason.name(), report.getReviewedAt()));
    return saved(report, "rejected", officerId);
  }

  /** Asks the reporter for more information; the report stays out of warnings until decided. */
  @Transactional
  public ReportDetailView requestInfo(UUID reportId, UUID officerId, String comment) {
    HazardReport report = load(reportId);
    report.requestInfo(officerId, comment, clock);
    return saved(report, "needs more info", officerId);
  }

  private HazardReport load(UUID reportId) {
    return reports
        .findWithLockById(reportId)
        .orElseThrow(() -> new NotFoundException("Report not found."));
  }

  private ReportDetailView saved(HazardReport report, String outcome, UUID officerId) {
    reports.save(report);
    log.info("Report {} {} by officer {}", report.getId(), outcome, officerId);
    return queries.viewOf(report, true);
  }
}
