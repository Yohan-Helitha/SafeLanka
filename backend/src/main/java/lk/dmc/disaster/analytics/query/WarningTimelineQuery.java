package lk.dmc.disaster.analytics.query;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.AlertTimeline.TimelineEntry;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.ContextFilters.Filter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The warnings of an event in time order. A warning counts when it was issued inside the window and
 * reaches one of the chosen districts, named directly or through a river basin that flows through it.
 */
@Component
@Transactional(readOnly = true)
public class WarningTimelineQuery {

  private final JdbcClient jdbcClient;

  public WarningTimelineQuery(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<TimelineEntry> execute(ReportContext context) {
    Filter window = ContextFilters.window(context, "w.issued_at");
    Filter districts = ContextFilters.districts(context, "td.district_id");
    Filter filter = window.and(districts);
    String districtCondition =
        districts.sql().isEmpty()
            ? ""
            : "AND EXISTS (SELECT 1 FROM target_districts td WHERE td.warning_id = w.id "
                + districts.sql()
                + ") ";

    String sql =
        "WITH target_basins AS ( "
            + "  SELECT warning_id, river_basin_id FROM warning_target_areas WHERE river_basin_id IS NOT NULL "
            + "), target_districts AS ( "
            + "  SELECT warning_id, district_id FROM warning_target_areas WHERE district_id IS NOT NULL "
            + "  UNION "
            + "  SELECT tb.warning_id, drb.district_id FROM target_basins tb "
            + "  JOIN district_river_basins drb ON tb.river_basin_id = drb.river_basin_id "
            + ") "
            + "SELECT w.id AS warningId, w.level, w.status, w.issued_at AS issuedAt, "
            + "w.supersedes_id AS supersedesId, "
            + "COALESCE((SELECT array_agg(DISTINCT td.district_id) FROM target_districts td "
            + "WHERE td.warning_id = w.id), CAST('{}' AS uuid[])) AS resolvedDistrictIds "
            + "FROM warnings w "
            + "WHERE w.event_id = :eventId "
            + window.sql()
            + districtCondition
            + "ORDER BY w.issued_at ASC";

    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query((rs, rowNum) -> entry(rs))
        .list();
  }

  private static TimelineEntry entry(java.sql.ResultSet rs) throws SQLException {
    return new TimelineEntry(
        rs.getObject("warningId", UUID.class),
        rs.getString("level"),
        rs.getString("status"),
        rs.getTimestamp("issuedAt").toInstant(),
        rs.getObject("supersedesId", UUID.class),
        (UUID[]) rs.getArray("resolvedDistrictIds").getArray());
  }
}
