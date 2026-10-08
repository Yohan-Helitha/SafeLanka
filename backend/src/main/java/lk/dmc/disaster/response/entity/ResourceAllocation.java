package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "resource_allocations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceAllocation {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "stock_id", nullable = false)
  private UUID stockId;

  @Column(name = "shelter_id", nullable = false)
  private UUID shelterId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "status", nullable = false, length = 25)
  private AllocationStatus status;

  @Column(name = "allocated_by", nullable = false)
  private UUID allocatedBy;

  @Column(name = "allocated_at", nullable = false)
  private Instant allocatedAt;

  @Column(name = "version", nullable = false)
  private long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static ResourceAllocation create(
      UUID stockId, UUID shelterId, UUID eventId, int quantity, UUID allocatedBy) {
    ResourceAllocation a = new ResourceAllocation();
    a.id = UUID.randomUUID();
    a.stockId = stockId;
    a.shelterId = shelterId;
    a.eventId = eventId;
    a.quantity = quantity;
    a.status = AllocationStatus.ALLOCATED;
    a.allocatedBy = allocatedBy;
    a.allocatedAt = Instant.now();
    a.version = 0L;
    return a;
  }

  public void markPartiallyDistributed() {
    this.status = AllocationStatus.PARTIALLY_DISTRIBUTED;
  }

  public void markDistributed() {
    this.status = AllocationStatus.DISTRIBUTED;
  }

  public void cancel() {
    this.status = AllocationStatus.CANCELLED;
  }
}
