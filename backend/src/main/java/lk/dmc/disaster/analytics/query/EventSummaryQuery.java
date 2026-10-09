package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.EventSummary;
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
                     "COALESCE((SELECT array_agg(district_id) FROM event_districts ed WHERE ed.event_id = e.id), CAST('{}' AS uuid[])) AS districtIds, " +
                     "(SELECT count(*) FROM warnings w WHERE w.event_id = e.id) AS warningCount, " +
                     "(SELECT count(*) FROM hazard_reports r JOIN hazard_evidence he ON r.id = he.report_id JOIN hazards h ON he.hazard_id = h.id WHERE h.event_id = e.id AND r.status = 'VERIFIED') AS reportCount " +
                     "FROM disaster_events e " +
                     "WHERE (CAST(:status AS VARCHAR) IS NULL OR e.status = :status) " +
                     "ORDER BY e.started_at DESC";
        return jdbcClient.sql(sql).param("status", status).query((rs, rowNum) -> {
            java.sql.Array arr = rs.getArray("districtIds");
            UUID[] districtIds = (UUID[]) arr.getArray();
            return new EventSummary(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getObject("hazardTypeId", UUID.class),
                rs.getString("status"),
                rs.getTimestamp("startedAt").toInstant(),
                rs.getTimestamp("endedAt") != null ? rs.getTimestamp("endedAt").toInstant() : null,
                districtIds,
                rs.getInt("warningCount"),
                rs.getInt("reportCount")
            );
        }).list();
    }
}
