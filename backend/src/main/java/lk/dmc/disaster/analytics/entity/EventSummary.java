package lk.dmc.disaster.analytics.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventSummary(
    UUID id, String name, UUID hazardTypeId, String status, Instant startedAt, Instant endedAt,
    UUID[] districtIds, int warningCount, int reportCount
) {}
