package lk.dmc.disaster.analytics.section;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lk.dmc.disaster.analytics.entity.AlertTimeline;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.SectionKey;
import lk.dmc.disaster.analytics.entity.SectionResult;
import lk.dmc.disaster.analytics.query.ReportTimingQuery;
import lk.dmc.disaster.analytics.query.WarningTimelineQuery;
import org.springframework.stereotype.Component;

/** Report section: the warnings issued during the event. */
@Component
public class AlertTimelineSection implements ReportSection {
    private final WarningTimelineQuery warningQuery;
    private final ReportTimingQuery timingQuery;

    public AlertTimelineSection(WarningTimelineQuery warningQuery, ReportTimingQuery timingQuery) {
        this.warningQuery = warningQuery;
        this.timingQuery = timingQuery;
    }

    /** This section is the {@link SectionKey#ALERT_TIMELINE}. */
    @Override
    public SectionKey getKey() { return SectionKey.ALERT_TIMELINE; }

    /** Lists the warnings of the event in time order, with the delay from the first verified report. */
    @Override
    public SectionResult<?> generate(ReportContext context) {
        List<AlertTimeline.TimelineEntry> entries = warningQuery.execute(context);
        if (entries.isEmpty()) {
            return SectionResult.unavailable(getKey(), "No warnings were issued for this event in the selected time window and districts.");
        }
        Instant firstVerified = timingQuery.execute(context).orElse(null);
        Instant firstWarning = entries.get(0).issuedAt();
        Long diff = (firstVerified != null) ? Duration.between(firstVerified, firstWarning).toMinutes() : null;
        return SectionResult.success(getKey(), new AlertTimeline(entries, firstVerified, firstWarning, diff));
    }
}
