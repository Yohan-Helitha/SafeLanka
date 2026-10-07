package lk.dmc.disaster.response.dto.response;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.response.entity.AllocationStatus;
import lk.dmc.disaster.response.entity.ResourceAllocation;

public record AllocationDto(
    UUID id,
    UUID stockId,
    UUID shelterId,
    UUID eventId,
    int quantity,
    AllocationStatus status,
    UUID allocatedBy,
    Instant allocatedAt,
    long version,
    Instant createdAt,
    Instant updatedAt) {

  public static AllocationDto from(ResourceAllocation allocation) {
    return new AllocationDto(
        allocation.getId(),
        allocation.getStockId(),
        allocation.getShelterId(),
        allocation.getEventId(),
        allocation.getQuantity(),
        allocation.getStatus(),
        allocation.getAllocatedBy(),
        allocation.getAllocatedAt(),
        allocation.getVersion(),
        allocation.getCreatedAt(),
        allocation.getUpdatedAt());
  }
}
