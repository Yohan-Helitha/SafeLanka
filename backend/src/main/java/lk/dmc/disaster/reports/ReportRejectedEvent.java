package lk.dmc.disaster.reports;

import java.time.Instant;
import java.util.UUID;

/** Published inside the reject transaction. No module listens yet (reserved). */
public record ReportRejectedEvent(UUID reportId, String reason, Instant rejectedAt) {}
