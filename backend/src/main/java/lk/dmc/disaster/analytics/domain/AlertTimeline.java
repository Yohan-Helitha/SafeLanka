package lk.dmc.disaster.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertTimeline(
    List<TimelineEntry> entries,
    Instant firstVerifiedReportAt,
    Instant firstWarningAt,
    Long reportToWarningMinutes
) {
    public record TimelineEntry(
        UUID warningId,
        String level,
        String status,
        Instant issuedAt,
        UUID supersedesId,
        List<UUID> resolvedDistrictIds
    ) {}
}
