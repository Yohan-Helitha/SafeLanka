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
@Table(name = "rescue_teams")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RescueTeam {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "organisation_id", nullable = false)
  private UUID organisationId;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "team_type", nullable = false, length = 10)
  private TeamType teamType;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RescueTeamStatus status;

  @Column(name = "last_status_at", nullable = false)
  private Instant lastStatusAt;

  @Column(name = "version", nullable = false)
  private long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static RescueTeam create(
      String name, UUID organisationId, UUID districtId, TeamType teamType, int capacity) {
    RescueTeam team = new RescueTeam();
    team.id = UUID.randomUUID();
    team.name = name;
    team.organisationId = organisationId;
    team.districtId = districtId;
    team.teamType = teamType;
    team.capacity = capacity;
    team.status = RescueTeamStatus.AVAILABLE;
    team.lastStatusAt = Instant.now();
    team.version = 0L;
    return team;
  }

  public void markDispatched() {
    this.status = RescueTeamStatus.DISPATCHED;
    this.lastStatusAt = Instant.now();
  }

  public void markEnRoute() {
    this.status = RescueTeamStatus.EN_ROUTE;
    this.lastStatusAt = Instant.now();
  }

  public void markActive() {
    this.status = RescueTeamStatus.ACTIVE;
    this.lastStatusAt = Instant.now();
  }

  public void markCompleted() {
    this.status = RescueTeamStatus.AVAILABLE;
    this.lastStatusAt = Instant.now();
  }

  public void markOfflineUnknown() {
    this.status = RescueTeamStatus.OFFLINE_UNKNOWN;
    this.lastStatusAt = Instant.now();
  }

  public void updateStatus(RescueTeamStatus newStatus) {
    this.status = newStatus;
    this.lastStatusAt = Instant.now();
  }
}
