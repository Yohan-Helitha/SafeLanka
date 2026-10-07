package lk.dmc.disaster.analytics.section;

import java.util.List;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.ResourceDistribution;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.DistributionQuery;
import org.springframework.stereotype.Component;

@Component
public class ResourceDistributionSection implements ReportSection {
    private final DistributionQuery query;

    public ResourceDistributionSection(DistributionQuery query) { this.query = query; }

    @Override
    public SectionKey getKey() { return SectionKey.RESOURCE_DISTRIBUTION; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        var byDistrict = query.getByDistrict(context);
        if (byDistrict.isEmpty()) {
            return SectionResult.unavailable(getKey(), "No resource allocations were found for this event.");
        }
        return SectionResult.success(getKey(), new ResourceDistribution(
            byDistrict, query.getByOrganisationType(context), query.getByItem(context)
        ));
    }
}
