package lk.dmc.disaster.analytics.section;

import java.util.List;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.ResourceDistribution;
import lk.dmc.disaster.analytics.entity.SectionKey;
import lk.dmc.disaster.analytics.entity.SectionResult;
import lk.dmc.disaster.analytics.query.DistributionQuery;
import org.springframework.stereotype.Component;

/** Report section: relief allocated and distributed. */
@Component
public class ResourceDistributionSection implements ReportSection {
    private final DistributionQuery query;

    public ResourceDistributionSection(DistributionQuery query) { this.query = query; }

    /** This section is the {@link SectionKey#RESOURCE_DISTRIBUTION}. */
    @Override
    public SectionKey getKey() { return SectionKey.RESOURCE_DISTRIBUTION; }

    /** Totals relief allocated and distributed by district, organisation type and item. */
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
