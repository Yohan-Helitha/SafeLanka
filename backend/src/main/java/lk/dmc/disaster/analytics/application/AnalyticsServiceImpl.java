package lk.dmc.disaster.analytics.application;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.AnalyticsRules;
import lk.dmc.disaster.analytics.domain.DisasterReport;
import lk.dmc.disaster.analytics.domain.EventSummary;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.persistence.DisasterReportRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ReportBuilder reportBuilder;
    private final DisasterReportRepository repository;
    private final JdbcClient jdbcClient;

    public AnalyticsServiceImpl(ReportBuilder reportBuilder, DisasterReportRepository repository, JdbcClient jdbcClient) {
        this.reportBuilder = reportBuilder;
        this.repository = repository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummary> listAvailableEvents(String status) {
        String sql = "SELECT e.id, e.name, e.hazard_type_id AS hazardTypeId, e.status, e.started_at AS startedAt, e.ended_at AS endedAt, " +
                     "COALESCE((SELECT array_agg(district_id) FROM event_districts ed WHERE ed.event_id = e.id), '{}') AS districtIds, " +
                     "(SELECT count(*) FROM warnings w WHERE w.event_id = e.id) AS warningCount, " +
                     "(SELECT count(*) FROM hazard_reports r JOIN hazard_evidence he ON r.id = he.report_id JOIN hazards h ON he.hazard_id = h.id WHERE h.event_id = e.id AND r.status = 'VERIFIED') AS reportCount " +
                     "FROM disaster_events e " +
                     "WHERE (:status IS NULL OR e.status = :status) " +
                     "ORDER BY e.started_at DESC";
        return jdbcClient.sql(sql)
            .param("status", status)
            .query(EventSummary.class)
            .list();
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
