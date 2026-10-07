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
        String sql = "SELECT COUNT(DISTINCT nd.citizen_id) FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds))";
        return queryLong(sql, context);
    }

    public long getUniqueReached(ReportContext context) {
        String sql = "SELECT COUNT(DISTINCT nd.citizen_id) FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId AND nd.status = 'DELIVERED' " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds))";
        return queryLong(sql, context);
    }

    public List<ChannelStats> getChannelStats(ReportContext context) {
        String sql = "SELECT nd.channel, " +
                     "COUNT(CASE WHEN nd.status = 'DELIVERED' THEN 1 END) AS delivered, " +
                     "COUNT(CASE WHEN nd.status != 'DELIVERED' THEN 1 END) AS failed " +
                     "FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds)) " +
                     "GROUP BY nd.channel ORDER BY nd.channel";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(ChannelStats.class).list();
    }

    public List<DistrictStats> getDistrictStats(ReportContext context) {
        String sql = "SELECT u.district_id AS districtId, d.name AS districtName, " +
                     "COUNT(DISTINCT nd.citizen_id) AS targeted, " +
                     "COUNT(DISTINCT CASE WHEN nd.status = 'DELIVERED' THEN nd.citizen_id END) AS reached " +
                     "FROM notification_deliveries nd " +
                     "JOIN warnings w ON nd.warning_id = w.id " +
                     "JOIN users u ON nd.citizen_id = u.id " +
                     "JOIN districts d ON u.district_id = d.id " +
                     "WHERE w.event_id = :eventId " +
                     "AND (:fromTime IS NULL OR nd.attempted_at >= :fromTime) " +
                     "AND (:toTime IS NULL OR nd.attempted_at <= :toTime) " +
                     "AND (COALESCE(array_length(:districtIds, 1), 0) = 0 OR u.district_id = ANY(:districtIds)) " +
                     "GROUP BY u.district_id, d.name ORDER BY d.name";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(DistrictStats.class).list();
    }

    private long queryLong(String sql, ReportContext context) {
        Long val = jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .param("fromTime", context.fromTime())
            .param("toTime", context.toTime())
            .param("districtIds", context.districtIds() == null ? new UUID[0] : context.districtIds().toArray(new UUID[0]))
            .query(Long.class).single();
        return val != null ? val : 0L;
    }
}
