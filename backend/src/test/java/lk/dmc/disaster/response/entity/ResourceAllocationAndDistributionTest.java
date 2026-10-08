package lk.dmc.disaster.response.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResourceAllocationAndDistributionTest {

  @Test
  void resourceAllocation_lifecycleAndTransitions() {
    UUID stockId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID allocatedBy = UUID.randomUUID();

    ResourceAllocation allocation =
        ResourceAllocation.create(stockId, shelterId, eventId, 250, allocatedBy);

    assertThat(allocation.getId()).isNotNull();
    assertThat(allocation.getStockId()).isEqualTo(stockId);
    assertThat(allocation.getShelterId()).isEqualTo(shelterId);
    assertThat(allocation.getEventId()).isEqualTo(eventId);
    assertThat(allocation.getQuantity()).isEqualTo(250);
    assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ALLOCATED);
    assertThat(allocation.getAllocatedBy()).isEqualTo(allocatedBy);
    assertThat(allocation.getAllocatedAt()).isNotNull();
    assertThat(allocation.getVersion()).isEqualTo(0L);

    allocation.markPartiallyDistributed();
    assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.PARTIALLY_DISTRIBUTED);

    allocation.markDistributed();
    assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.DISTRIBUTED);

    allocation.cancel();
    assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.CANCELLED);
  }

  @Test
  void reliefDistribution_creation() {
    UUID allocationId = UUID.randomUUID();
    UUID distributedBy = UUID.randomUUID();
    UUID clientRef = UUID.randomUUID();
    Instant distributedAt = Instant.now().minusSeconds(3600);

    ReliefDistribution distribution =
        ReliefDistribution.create(allocationId, 100, distributedBy, distributedAt, true, clientRef);

    assertThat(distribution.getId()).isNotNull();
    assertThat(distribution.getAllocationId()).isEqualTo(allocationId);
    assertThat(distribution.getQuantityDistributed()).isEqualTo(100);
    assertThat(distribution.getDistributedBy()).isEqualTo(distributedBy);
    assertThat(distribution.getDistributedAt()).isEqualTo(distributedAt);
    assertThat(distribution.isRecordedOffline()).isTrue();
    assertThat(distribution.getClientRef()).isEqualTo(clientRef);
    assertThat(distribution.getSyncedAt()).isNotNull();
  }
}
