package lk.dmc.disaster.analytics.query;

import java.util.List;
import lk.dmc.disaster.analytics.domain.ResourceDistribution;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ResourceDistributionQuery implements SectionQuery<ResourceDistribution> {
    private final JdbcClient jdbcClient;
    public ResourceDistributionQuery(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    @Override
    public SectionResult<ResourceDistribution> execute(ReportContext context) {
        String sql = "SELECT a.id as allocationId, a.quantity as quantityAllocated, COALESCE(SUM(d.quantity_distributed), 0) as quantityDistributed FROM resource_allocations a LEFT JOIN relief_distributions d ON a.id = d.allocation_id WHERE a.event_id = :eventId GROUP BY a.id, a.quantity";
            
        List<ResourceDistribution.DistributionRecord> records = jdbcClient.sql(sql)
            .param("eventId", context.eventId())
            .query(ResourceDistribution.DistributionRecord.class)
            .list();
            
        if (records.isEmpty()) {
            return SectionResult.unavailable(SectionKey.RESOURCE_DISTRIBUTION, "No resources allocated");
        }
        return SectionResult.success(SectionKey.RESOURCE_DISTRIBUTION, new ResourceDistribution(records));
    }
}
