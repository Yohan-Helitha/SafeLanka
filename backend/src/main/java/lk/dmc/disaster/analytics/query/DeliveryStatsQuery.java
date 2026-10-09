package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.entity.CitizensReached.ChannelStats;
import lk.dmc.disaster.analytics.entity.CitizensReached.DistrictStats;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.ContextFilters.Filter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who a warning was sent to and who it reached, from the notification deliveries of an event. A
 * delivery counts when it was attempted inside the window and the citizen lives in a chosen district.
 */
@Component
@Transactional(readOnly = true)
public class DeliveryStatsQuery {

  private static final String FROM =
      "FROM notification_deliveries nd "
          + "JOIN warnings w ON nd.warning_id = w.id "
          + "JOIN users u ON nd.citizen_id = u.id ";

  private final JdbcClient jdbcClient;

  public DeliveryStatsQuery(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /** Distinct citizens any warning of the event was sent to. */
  public long getUniqueTargeted(ReportContext context) {
    return countCitizens("WHERE w.event_id = :eventId ", context);
  }

  /** Distinct citizens with at least one DELIVERED notification. */
  public long getUniqueReached(ReportContext context) {
    return countCitizens("WHERE w.event_id = :eventId AND nd.status = 'DELIVERED' ", context);
  }

  public List<ChannelStats> getChannelStats(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "SELECT nd.channel, "
            + "COUNT(CASE WHEN nd.status = 'DELIVERED' THEN 1 END) AS delivered, "
            + "COUNT(CASE WHEN nd.status != 'DELIVERED' THEN 1 END) AS failed "
            + FROM
            + "WHERE w.event_id = :eventId "
            + filter.sql()
            + "GROUP BY nd.channel ORDER BY nd.channel";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(ChannelStats.class)
        .list();
  }

  public List<DistrictStats> getDistrictStats(ReportContext context) {
    Filter filter = filter(context);
    String sql =
        "SELECT u.district_id AS districtId, d.name AS districtName, "
            + "COUNT(DISTINCT nd.citizen_id) AS targeted, "
            + "COUNT(DISTINCT CASE WHEN nd.status = 'DELIVERED' THEN nd.citizen_id END) AS reached "
            + FROM
            + "JOIN districts d ON u.district_id = d.id "
            + "WHERE w.event_id = :eventId "
            + filter.sql()
            + "GROUP BY u.district_id, d.name ORDER BY d.name";
    return ContextFilters.statement(jdbcClient, sql, context, filter)
        .query(DistrictStats.class)
        .list();
  }

  private long countCitizens(String where, ReportContext context) {
    Filter filter = filter(context);
    String sql = "SELECT COUNT(DISTINCT nd.citizen_id) " + FROM + where + filter.sql();
    return ContextFilters.statement(jdbcClient, sql, context, filter).query(Long.class).single();
  }

  private static Filter filter(ReportContext context) {
    return ContextFilters.of(context, "nd.attempted_at", "u.district_id");
  }
}
