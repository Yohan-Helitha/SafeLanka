package lk.dmc.disaster.analytics.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.AnalyticsRules;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.analytics.entity.EventSummary;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.EventSummaryQuery;
import lk.dmc.disaster.analytics.repository.DisasterReportRepository;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.NotFoundException;
import lk.dmc.disaster.shared.reference.DisasterEventView;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC04: lists events, generates and saves reports, and reopens saved ones. */
@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

  private final ReportBuilder reportBuilder;
  private final DisasterReportRepository repository;
  private final EventSummaryQuery eventSummaryQuery;
  private final ReferenceData referenceData;
  private final Clock clock;

  public AnalyticsServiceImpl(
      ReportBuilder reportBuilder,
      DisasterReportRepository repository,
      EventSummaryQuery eventSummaryQuery,
      ReferenceData referenceData,
      Clock clock) {
    this.reportBuilder = reportBuilder;
    this.repository = repository;
    this.eventSummaryQuery = eventSummaryQuery;
    this.referenceData = referenceData;
    this.clock = clock;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public List<EventSummary> listAvailableEvents(String status) {
    return eventSummaryQuery.execute(status);
  }

  /**
   * Builds and saves a report. Districts default to all the event's districts and the window to the
   * event's own start and end (or now, while it is still active).
   *
   * @throws NotFoundException (404) when the event does not exist; nothing is saved
   * @throws BusinessRuleException (422) for districts outside the event, a start after the end, or
   *     a window outside the event
   */
  @Override
  @Transactional
  public DisasterReport generateReport(ReportContext requested) {
    DisasterEventView event = referenceData.event(requested.eventId());
    ReportContext context = withDefaultsAndChecks(requested, event);
    log.info(
        "Generating report for event {} (districts {}, {} to {})",
        context.eventId(),
        context.districtIds(),
        context.fromTime(),
        context.toTime());
    DisasterReport saved = repository.save(reportBuilder.build(context));
    log.info("Report {} saved for event {}", saved.getId(), context.eventId());
    return saved;
  }

  private ReportContext withDefaultsAndChecks(ReportContext requested, DisasterEventView event) {
    Set<UUID> districts =
        requested.districtIds() == null || requested.districtIds().isEmpty()
            ? event.districtIds()
            : requested.districtIds();
    if (!event.districtIds().containsAll(districts)) {
      throw new BusinessRuleException("Some of the chosen districts are not part of this event.");
    }

    Instant eventStart = event.startedAt().toInstant();
    Instant eventEnd = event.endedAt() != null ? event.endedAt().toInstant() : clock.instant();
    Instant from = requested.fromTime() != null ? requested.fromTime() : eventStart;
    Instant to = requested.toTime() != null ? requested.toTime() : eventEnd;
    if (from.isAfter(to)) {
      throw new BusinessRuleException("The start of the time window must not be after its end.");
    }
    if (from.isBefore(eventStart.minus(AnalyticsRules.WINDOW_TOLERANCE))
        || to.isAfter(eventEnd.plus(AnalyticsRules.WINDOW_TOLERANCE))) {
      throw new BusinessRuleException("The time window must lie inside the event.");
    }
    return new ReportContext(
        requested.eventId(), Set.copyOf(districts), from, to, requested.generatedBy());
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public List<DisasterReport> listSavedReports(UUID eventId) {
    return eventId != null
        ? repository.findByEventIdOrderByGeneratedAtDesc(eventId)
        : repository.findAllByOrderByGeneratedAtDesc();
  }

  /** @throws NotFoundException (404) when no saved report has this id */
  @Override
  @Transactional(readOnly = true)
  public DisasterReport getReport(UUID reportId) {
    return repository
        .findById(reportId)
        .orElseThrow(() -> new NotFoundException("Report not found."));
  }
}
