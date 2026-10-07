package lk.dmc.disaster.analytics.section;

import lk.dmc.disaster.analytics.domain.ReportContext;
import lk.dmc.disaster.analytics.domain.SectionKey;
import lk.dmc.disaster.analytics.domain.SectionResult;
import lk.dmc.disaster.analytics.query.AlertTimelineQuery;
import org.springframework.stereotype.Component;

@Component
public class AlertTimelineSection implements ReportSection {
    private final AlertTimelineQuery query;

    public AlertTimelineSection(AlertTimelineQuery query) {
        this.query = query;
    }

    @Override
    public SectionKey getKey() {
        return SectionKey.ALERT_TIMELINE;
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
