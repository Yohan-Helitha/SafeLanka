package lk.dmc.disaster.analytics.domain;

import java.time.Instant;
import java.util.List;

public record AlertTimeline(List<TimelineEvent> events) {
    public record TimelineEvent(
        Instant timestamp,
        String level,
        String title,
        String status
    ) {}
}
