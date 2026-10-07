package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "relief_distributions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReliefDistribution {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "allocation_id", nullable = false)
  private UUID allocationId;

  @Column(name = "quantity_distributed", nullable = false)
  private int quantityDistributed;

  @Column(name = "distributed_by", nullable = false)
  private UUID distributedBy;

  @Column(name = "distributed_at", nullable = false)
  private Instant distributedAt;

  @Column(name = "recorded_offline", nullable = false)
  private boolean recordedOffline;

  @Column(name = "client_ref")
  private UUID clientRef;

  @Column(name = "synced_at", nullable = false)
  private Instant syncedAt;

  public static ReliefDistribution create(
      UUID allocationId,
      int quantityDistributed,
      UUID distributedBy,
      Instant distributedAt,
      boolean recordedOffline,
      UUID clientRef) {
    ReliefDistribution d = new ReliefDistribution();
    d.id = UUID.randomUUID();
    d.allocationId = allocationId;
    d.quantityDistributed = quantityDistributed;
    d.distributedBy = distributedBy;
    d.distributedAt = distributedAt;
    d.recordedOffline = recordedOffline;
    d.clientRef = clientRef;
    d.syncedAt = Instant.now();
    return d;
  }
}
