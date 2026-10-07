package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.domain.AlertTimeline;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AlertTimelineQuery implements SectionQuery<AlertTimeline> {

    private final JdbcClient jdbcClient;

    public AlertTimelineQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public SectionResult<AlertTimeline> execute(ReportContext context) {
        String sql = "SELECT issued_at as timestamp, level, title, status FROM warnings WHERE event_id = :eventId ORDER BY issued_at ASC";
        
        List<AlertTimeline.TimelineEvent> events = jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .query(AlertTimeline.TimelineEvent.class)
            .list();
            
        if (events.isEmpty()) {
            return SectionResult.unavailable(SectionKey.ALERT_TIMELINE, "No warnings found for this event");
        }
        return SectionResult.success(SectionKey.ALERT_TIMELINE, new AlertTimeline(events));
    }
}
