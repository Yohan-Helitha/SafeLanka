package lk.dmc.disaster.analytics.query;

import lk.dmc.disaster.analytics.domain.CitizensReached;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CitizensReachedQuery implements SectionQuery<CitizensReached> {
    private final JdbcClient jdbcClient;
    public CitizensReachedQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    @Override
    public SectionResult<CitizensReached> execute(ReportContext context) {
        String sql = "SELECT COUNT(*) as totalAttempted, COUNT(CASE WHEN d.status = 'DELIVERED' THEN 1 END) as totalDelivered, COUNT(CASE WHEN d.status = 'FAILED' THEN 1 END) as totalFailed FROM notification_deliveries d JOIN warnings w ON d.warning_id = w.id WHERE w.event_id = :eventId";
            
        var stats = jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .query(CitizensReached.class)
            .single();
            
        if (stats.totalAttempted() == 0) {
            return SectionResult.unavailable(SectionKey.CITIZENS_REACHED, "No notifications sent");
        }
        return SectionResult.success(SectionKey.CITIZENS_REACHED, stats);
    }
}
