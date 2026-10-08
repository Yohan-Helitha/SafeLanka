package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.response.entity.ReliefDistribution;

public record DistributionDto(
    UUID id,
    UUID allocationId,
    int quantityDistributed,
    UUID distributedBy,
    Instant distributedAt,
    boolean recordedOffline,
    UUID clientRef,
    Instant syncedAt,
    Instant createdAt) {

  public static DistributionDto from(ReliefDistribution distribution) {
    return new DistributionDto(
        distribution.getId(),
        distribution.getAllocationId(),
        distribution.getQuantityDistributed(),
        distribution.getDistributedBy(),
        distribution.getDistributedAt(),
        distribution.isRecordedOffline(),
        distribution.getClientRef(),
        distribution.getSyncedAt(),
        distribution.getSyncedAt());
  }
}
