package lk.dmc.disaster.analytics.query;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.analytics.domain.CitizensReached.ChannelStats;
import lk.dmc.disaster.analytics.domain.CitizensReached.DistrictStats;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DeliveryStatsQuery {
    private final JdbcClient jdbcClient;
    public DeliveryStatsQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public long getUniqueTargeted(ReportContext context) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT nd.citizen_id) FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId ");
        appendFilters(sql, context);
        return queryLong(sql.toString(), context);
    }

    public long getUniqueReached(ReportContext context) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT nd.citizen_id) FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId AND nd.status = 'DELIVERED' ");
        appendFilters(sql, context);
        return queryLong(sql.toString(), context);
    }

    public List<ChannelStats> getChannelStats(ReportContext context) {
        StringBuilder sql = new StringBuilder("SELECT nd.channel, " +
                     "COUNT(CASE WHEN nd.status = 'DELIVERED' THEN 1 END) AS delivered, " +
                     "COUNT(CASE WHEN nd.status != 'DELIVERED' THEN 1 END) AS failed " +
                     "FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId ");
        appendFilters(sql, context);
        sql.append("GROUP BY nd.channel ORDER BY nd.channel");
        
        var query = bindParameters(jdbcClient.sql(sql.toString()), context);
        return query.query(ChannelStats.class).list();
    }

    public List<DistrictStats> getDistrictStats(ReportContext context) {
        StringBuilder sql = new StringBuilder("SELECT u.district_id AS districtId, d.name AS districtName, " +
                     "COUNT(DISTINCT nd.citizen_id) AS targeted, " +
                     "COUNT(DISTINCT CASE WHEN nd.status = 'DELIVERED' THEN nd.citizen_id END) AS reached " +
                     "FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "JOIN districts d ON u.district_id = d.id " +
                     "WHERE w.event_id = :eventId ");
        appendFilters(sql, context);
        sql.append("GROUP BY u.district_id, d.name ORDER BY d.name");
        
        var query = bindParameters(jdbcClient.sql(sql.toString()), context);
        return query.query(DistrictStats.class).list();
    }

    private void appendFilters(StringBuilder sql, ReportContext context) {
        if (context.fromTime() != null) sql.append("AND nd.attempted_at >= :fromTime ");
        if (context.toTime() != null) sql.append("AND nd.attempted_at <= :toTime ");
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            sql.append("AND u.district_id = ANY(CAST(:districtIds AS uuid[])) ");
        }
    }

    private org.springframework.jdbc.core.simple.JdbcClient.StatementSpec bindParameters(org.springframework.jdbc.core.simple.JdbcClient.StatementSpec spec, ReportContext context) {
        var query = spec.param("eventId", context.eventId());
        if (context.fromTime() != null) query = query.param("fromTime", java.time.OffsetDateTime.ofInstant(context.fromTime(), java.time.ZoneOffset.UTC));
        if (context.toTime() != null) query = query.param("toTime", java.time.OffsetDateTime.ofInstant(context.toTime(), java.time.ZoneOffset.UTC));
        if (context.districtIds() != null && !context.districtIds().isEmpty()) {
            query = query.param("districtIds", context.districtIds().toArray(new UUID[0]));
        }
        return query;
    }

    private long queryLong(String sql, ReportContext context) {
        var query = bindParameters(jdbcClient.sql(sql), context);
        Long val = query.query(Long.class).single();
        return val != null ? val : 0L;
    }
}
