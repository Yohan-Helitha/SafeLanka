package lk.dmc.disaster.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventSummary(
    UUID id, String name, UUID hazardTypeId, String status, Instant startedAt, Instant endedAt,
    List<UUID> districtIds, int warningCount, int reportCount
) {}
