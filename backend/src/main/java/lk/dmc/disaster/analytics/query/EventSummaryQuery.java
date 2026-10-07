package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.domain.EventSummary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class EventSummaryQuery {
    private final JdbcClient jdbcClient;
    public EventSummaryQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<EventSummary> execute(String status) {
        String sql = "SELECT e.id, e.name, e.hazard_type_id AS hazardTypeId, e.status, e.started_at AS startedAt, e.ended_at AS endedAt, " +
                     "COALESCE((SELECT array_agg(district_id) FROM event_districts ed WHERE ed.event_id = e.id), '{}') AS districtIds, " +
                     "(SELECT count(*) FROM warnings w WHERE w.event_id = e.id) AS warningCount, " +
                     "(SELECT count(*) FROM hazard_reports r JOIN hazard_evidence he ON r.id = he.report_id JOIN hazards h ON he.hazard_id = h.id WHERE h.event_id = e.id AND r.status = 'VERIFIED') AS reportCount " +
                     "FROM disaster_events e " +
                     "WHERE (:status IS NULL OR e.status = :status) " +
                     "ORDER BY e.started_at DESC";
        return jdbcClient.sql(sql).param("status", status).query(EventSummary.class).list();
    }
}
