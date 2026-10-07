package lk.dmc.disaster.analytics.application;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.AnalyticsRules;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.EventSummary;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.persistence.DisasterReportRepository;
import lk.dmc.disaster.analytics.query.EventSummaryQuery;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ReportBuilder reportBuilder;
    private final DisasterReportRepository repository;
    private final EventSummaryQuery eventSummaryQuery;
    private final JdbcClient jdbcClient;

    public AnalyticsServiceImpl(ReportBuilder reportBuilder, DisasterReportRepository repository, 
                              EventSummaryQuery eventSummaryQuery, JdbcClient jdbcClient) {
        this.reportBuilder = reportBuilder;
        this.repository = repository;
        this.eventSummaryQuery = eventSummaryQuery;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummary> listAvailableEvents(String status) {
        return eventSummaryQuery.execute(status);
    }

    @Override
    @Transactional
    public DisasterReport generateReport(ReportContext context) {
        validateContext(context);
        
        DisasterReport report = reportBuilder.build(context);
        return repository.save(report);
    }

    private void validateContext(ReportContext context) {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM disaster_events WHERE id = :id")
            .param("id", context.eventId())
            .query(Integer.class)
            .single();
            
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Unknown event");
        }
        
        if (context.fromTime() != null && context.toTime() != null) {
            Duration duration = Duration.between(context.fromTime(), context.toTime());
            if (duration.compareTo(AnalyticsRules.MAX_TIME_WINDOW) > 0) {
                throw new IllegalArgumentException("Time range exceeds maximum allowed window");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisasterReport> listSavedReports(UUID eventId) {
        return repository.findByEventIdOrderByGeneratedAtDesc(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public DisasterReport getReport(UUID reportId) {
        return repository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }
}
