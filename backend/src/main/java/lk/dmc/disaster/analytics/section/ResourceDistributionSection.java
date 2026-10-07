package lk.dmc.disaster.analytics.section;

import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.ResourceDistributionQuery;
import org.springframework.stereotype.Component;

@Component
public class ResourceDistributionSection implements ReportSection {
    private final ResourceDistributionQuery query;

    public ResourceDistributionSection(ResourceDistributionQuery query) {
        this.query = query;
    }

    @Override
    public SectionKey getKey() {
        return SectionKey.RESOURCE_DISTRIBUTION;
    }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        try {
            return query.execute(context);
        } catch (Exception e) {
            return SectionResult.unavailable(getKey(), "Internal error generating section data");
        }
    }
}
