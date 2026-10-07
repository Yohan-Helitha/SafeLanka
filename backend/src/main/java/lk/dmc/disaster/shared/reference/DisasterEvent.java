package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** Disaster event entity. */
@Entity
@Table(name = "disaster_events")
public class DisasterEvent extends BaseEntity {
    private String name;
    private UUID hazardTypeId;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;

    @ManyToMany
    @JoinTable(
        name = "event_districts",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "district_id")
    )
    private Set<District> districts;

    protected DisasterEvent() {}

    public String getName() { return name; }
    public UUID getHazardTypeId() { return hazardTypeId; }
    public EventStatus getStatus() { return status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public Set<District> getDistricts() { return districts; }
}
