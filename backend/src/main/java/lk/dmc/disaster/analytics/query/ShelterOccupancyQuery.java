package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.domain.ShelterOccupancy;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ShelterOccupancyQuery implements SectionQuery<ShelterOccupancy> {
    private final JdbcClient jdbcClient;
    public ShelterOccupancyQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    @Override
    public SectionResult<ShelterOccupancy> execute(ReportContext context) {
        String sql = "SELECT shelter_id as shelterId, occupancy, recorded_at as recordedAt FROM occupancy_logs WHERE event_id = :eventId ORDER BY recorded_at ASC";
            
        List<ShelterOccupancy.ShelterLog> logs = jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .query(ShelterOccupancy.ShelterLog.class)
            .list();
            
        if (logs.isEmpty()) {
            return SectionResult.unavailable(SectionKey.SHELTER_OCCUPANCY, "No shelter data");
        }
        return SectionResult.success(SectionKey.SHELTER_OCCUPANCY, new ShelterOccupancy(logs));
    }
}
