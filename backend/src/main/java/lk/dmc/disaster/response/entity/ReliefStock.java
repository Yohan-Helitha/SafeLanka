package lk.dmc.disaster.response.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "relief_stocks")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReliefStock {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(name = "organisation_id", nullable = false)
  private UUID organisationId;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Column(name = "quantity_available", nullable = false)
  private int quantityAvailable;

  @Column(name = "version", nullable = false)
  private long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static ReliefStock create(
      UUID itemId, UUID organisationId, UUID districtId, int quantityAvailable) {
    ReliefStock s = new ReliefStock();
    s.id = UUID.randomUUID();
    s.itemId = itemId;
    s.organisationId = organisationId;
    s.districtId = districtId;
    s.quantityAvailable = quantityAvailable;
    s.version = 0L;
    return s;
  }

  public void decrease(int quantity) {
    if (quantity > quantityAvailable) {
      throw new IllegalArgumentException("Insufficient stock");
    }
    this.quantityAvailable -= quantity;
  }

  public void increase(int quantity) {
    this.quantityAvailable += quantity;
  }
}
