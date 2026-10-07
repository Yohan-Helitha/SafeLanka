package lk.dmc.disaster.analytics.domain;

import java.util.List;
import java.util.UUID;

public record ResourceDistribution(List<DistributionRecord> records) {
    public record DistributionRecord(
        UUID allocationId,
        int quantityAllocated,
        int quantityDistributed
    ) {}
}
