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
@Table(name = "shelters")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shelter {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Column(name = "address", nullable = false, length = 200)
  private String address;

  @Column(name = "latitude", nullable = false)
  private double latitude;

  @Column(name = "longitude", nullable = false)
  private double longitude;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Column(name = "current_occupancy", nullable = false)
  private int currentOccupancy;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private ShelterStatus status;

  @Column(name = "coordinator_id")
  private UUID coordinatorId;

  @Column(name = "version", nullable = false)
  private long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static Shelter create(
      String name,
      UUID districtId,
      String address,
      double latitude,
      double longitude,
      int capacity,
      UUID coordinatorId) {
    Shelter s = new Shelter();
    s.id = UUID.randomUUID();
    s.name = name;
    s.districtId = districtId;
    s.address = address;
    s.latitude = latitude;
    s.longitude = longitude;
    s.capacity = capacity;
    s.currentOccupancy = 0;
    s.status = ShelterStatus.OPEN;
    s.coordinatorId = coordinatorId;
    s.version = 0L;
    return s;
  }

  public int getAvailableCapacity() {
    return capacity - currentOccupancy;
  }

  public boolean isOpen() {
    return status == ShelterStatus.OPEN;
  }

  public void updateOccupancy(int newOccupancy) {
    if (newOccupancy > capacity) {
      throw new IllegalArgumentException("Occupancy exceeds capacity");
    }
    if (newOccupancy < 0) {
      throw new IllegalArgumentException("Occupancy cannot be negative");
    }
    this.currentOccupancy = newOccupancy;
    this.status = currentOccupancy >= capacity ? ShelterStatus.FULL : ShelterStatus.OPEN;
  }

  public void close() {
    this.status = ShelterStatus.CLOSED;
  }

  public void reopen() {
    this.status = ShelterStatus.OPEN;
  }
}
