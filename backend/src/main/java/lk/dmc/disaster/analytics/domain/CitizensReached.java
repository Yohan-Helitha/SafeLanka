package lk.dmc.disaster.analytics.domain;

public record CitizensReached(
    long totalAttempted,
    long totalDelivered,
    long totalFailed
) {}
