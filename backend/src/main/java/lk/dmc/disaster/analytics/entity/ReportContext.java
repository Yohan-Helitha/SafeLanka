package lk.dmc.disaster.analytics.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ReportContext(
    UUID eventId,
    Set<UUID> districtIds,
    Instant fromTime,
    Instant toTime,
    UUID generatedBy
) {
    public boolean isGlobal() {
        return districtIds == null || districtIds.isEmpty();
    }
}
