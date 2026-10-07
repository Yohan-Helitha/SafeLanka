package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.AlertTimeline.TimelineEntry;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class WarningTimelineQuery {
    private final JdbcClient jdbcClient;
    public WarningTimelineQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<TimelineEntry> execute(ReportContext context) {
        String sql = "WITH target_basins AS ( " +
                     "  SELECT warning_id, river_basin_id FROM warning_target_areas WHERE river_basin_id IS NOT NULL " +
                     "), target_districts AS ( " +
                     "  SELECT warning_id, district_id FROM warning_target_areas WHERE district_id IS NOT NULL " +
                     "  UNION " +
                     "  SELECT tb.warning_id, drb.district_id FROM target_basins tb JOIN district_river_basins drb ON tb.river_basin_id = drb.river_basin_id " +
                     ") " +
                     "SELECT w.id AS warningId, w.level, w.status, w.issued_at AS issuedAt, w.supersedes_id AS supersedesId, " +
                     "COALESCE((SELECT array_agg(DISTINCT td.district_id) FROM target_districts td WHERE td.warning_id = w.id), '{}') AS resolvedDistrictIds " +
                     "FROM warnings w " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR w.issued_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR w.issued_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR EXISTS (SELECT 1 FROM target_districts td WHERE td.warning_id = w.id AND td.district_id = ANY(:districtIds))) " +
                     "ORDER BY w.issued_at ASC";
        
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(TimelineEntry.class).list();
    }
}
