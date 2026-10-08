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
        StringBuilder sql = new StringBuilder(
            "WITH target_basins AS ( " +
            "  SELECT warning_id, river_basin_id FROM warning_target_areas WHERE river_basin_id IS NOT NULL " +
            "), target_districts AS ( " +
            "  SELECT warning_id, district_id FROM warning_target_areas WHERE district_id IS NOT NULL " +
            "  UNION " +
            "  SELECT tb.warning_id, drb.district_id FROM target_basins tb JOIN district_river_basins drb ON tb.river_basin_id = drb.river_basin_id " +
            ") " +
            "SELECT w.id AS warningId, w.level, w.status, w.issued_at AS issuedAt, w.supersedes_id AS supersedesId, " +
            "COALESCE((SELECT array_agg(DISTINCT td.district_id) FROM target_districts td WHERE td.warning_id = w.id), CAST('{}' AS uuid[])) AS resolvedDistrictIds " +
            "FROM warnings w " +
            "WHERE w.event_id = :eventId "
        );

        if (context.fromTime() != null) sql.append("AND w.issued_at >= :fromTime ");
        if (context.toTime() != null) sql.append("AND w.issued_at <= :toTime ");
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            sql.append("AND EXISTS (SELECT 1 FROM target_districts td WHERE td.warning_id = w.id AND td.district_id = ANY(CAST(:districtIds AS uuid[]))) ");
        }
        
        sql.append("ORDER BY w.issued_at ASC");
        
        var query = jdbcClient.sql(sql.toString()).param("eventId", context.eventId());
        
        if (context.fromTime() != null) query = query.param("fromTime", java.time.OffsetDateTime.ofInstant(context.fromTime(), java.time.ZoneOffset.UTC));
        if (context.toTime() != null) query = query.param("toTime", java.time.OffsetDateTime.ofInstant(context.toTime(), java.time.ZoneOffset.UTC));
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            query = query.param("districtIds", context.districtIds().toArray(new UUID[0]));
        }

        return query.query((rs, rowNum) -> {
            java.sql.Array arr = rs.getArray("resolvedDistrictIds");
                UUID[] resolved = arr == null ? new UUID[0] : (UUID[]) arr.getArray();
                return new TimelineEntry(
                    rs.getObject("warningId", UUID.class),
                    rs.getString("level"),
                    rs.getString("status"),
                    rs.getTimestamp("issuedAt") != null ? rs.getTimestamp("issuedAt").toInstant() : null,
                    rs.getObject("supersedesId", UUID.class),
                    resolved
                );
            }).list();
    }
}
