package lk.dmc.disaster.analytics.query;

import java.time.Instant;
import java.util.Optional;
import lk.dmc.disaster.analytics.domain.ReportContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ReportTimingQuery {
    private final JdbcClient jdbcClient;
    public ReportTimingQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public Optional<Instant> execute(ReportContext context) {
        String sql = "SELECT MIN(hr.reviewed_at) " +
                     "FROM hazard_reports hr " +
                     "JOIN hazard_evidence he ON hr.id = he.report_id " +
                     "JOIN hazards h ON he.hazard_id = h.id " +
                     "WHERE h.event_id = :eventId AND hr.status = 'VERIFIED'";
        return jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .query(Instant.class)
            .optional();
    }
}
