package lk.dmc.disaster.analytics.section;

import lk.dmc.disaster.analytics.domain.CitizensReached;
import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.DeliveryStatsQuery;
import org.springframework.stereotype.Component;

@Component
public class CitizensReachedSection implements ReportSection {
    private final DeliveryStatsQuery query;

    public CitizensReachedSection(DeliveryStatsQuery query) { this.query = query; }

    @Override
    public SectionKey getKey() { return SectionKey.CITIZENS_REACHED; }

    @Override
    public SectionResult<?> generate(ReportContext context) {
        long targeted = query.getUniqueTargeted(context);
        if (targeted == 0) {
            return SectionResult.unavailable(getKey(), "No notification deliveries were recorded for this event.");
        }
        long reached = query.getUniqueReached(context);
        double rate = (double) reached / targeted;
        // round to 2 decimals
        rate = Math.round(rate * 100.0) / 100.0;
        return SectionResult.success(getKey(), new CitizensReached(
            targeted, reached, rate, query.getChannelStats(context), query.getDistrictStats(context)
        ));
    }
}
