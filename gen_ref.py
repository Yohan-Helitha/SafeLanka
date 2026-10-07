import os

base_dir = r"c:\Users\Administrator\Desktop\3 Year\2nd sem\CSSE\Project\SafeLanka\backend\src\main\java\lk\dmc\disaster\shared\reference"
os.makedirs(base_dir, exist_ok=True)

files = {
    "OnsetSpeed.java": """package lk.dmc.disaster.shared.reference;

public enum OnsetSpeed {
    RAPID, SLOW
}
""",
    "OrganisationType.java": """package lk.dmc.disaster.shared.reference;

public enum OrganisationType {
    GOVERNMENT, ARMED_FORCES, POLICE, NGO, PRIVATE_DONOR
}
""",
    "ReliefCategory.java": """package lk.dmc.disaster.shared.reference;

public enum ReliefCategory {
    FOOD, WATER, MEDICINE, HYGIENE, SHELTER_KIT
}
""",
    "EventStatus.java": """package lk.dmc.disaster.shared.reference;

public enum EventStatus {
    ACTIVE, CLOSED
}
""",
    "District.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** District entity. */
@Entity
@Table(name = "districts")
public class District extends BaseEntity {
    private String code;
    private String name;
    private String province;
    
    protected District() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getProvince() { return province; }
}
""",
    "RiverBasin.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** River basin entity. */
@Entity
@Table(name = "river_basins")
public class RiverBasin extends BaseEntity {
    private String code;
    private String name;

    protected RiverBasin() {}

    public String getCode() { return code; }
    public String getName() { return name; }
}
""",
    "HazardType.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import lk.dmc.disaster.shared.domain.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Hazard type entity. */
@Entity
@Table(name = "hazard_types")
public class HazardType extends BaseEntity {
    private String code;
    private String name;

    @Enumerated(EnumType.STRING)
    private OnsetSpeed onsetSpeed;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> reportCategories;

    private boolean active;

    protected HazardType() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public OnsetSpeed getOnsetSpeed() { return onsetSpeed; }
    public List<String> getReportCategories() { return reportCategories; }
    public boolean isActive() { return active; }
}
""",
    "Organisation.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** Organisation entity. */
@Entity
@Table(name = "organisations")
public class Organisation extends BaseEntity {
    private String name;

    @Enumerated(EnumType.STRING)
    private OrganisationType type;

    private String contactPhone;

    protected Organisation() {}

    public String getName() { return name; }
    public OrganisationType getType() { return type; }
    public String getContactPhone() { return contactPhone; }
}
""",
    "ReliefItem.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** Relief item entity. */
@Entity
@Table(name = "relief_items")
public class ReliefItem extends BaseEntity {
    private String code;
    private String name;
    private String unit;

    @Enumerated(EnumType.STRING)
    private ReliefCategory category;

    protected ReliefItem() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public ReliefCategory getCategory() { return category; }
}
""",
    "DisasterEvent.java": """package lk.dmc.disaster.shared.reference;

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
""",
    "AppUser.java": """package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.BaseEntity;
import lk.dmc.disaster.shared.domain.Role;

/** App user entity mapping to users table. */
@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private Role role;

    private String fullName;
    private String phone;
    private String nic;
    private String homeAddress;
    
    private UUID districtId;
    private UUID riverBasinId;
    private String preferredLanguage;
    private UUID organisationId;
    private UUID rescueTeamId;

    protected AppUser() {}

    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getNic() { return nic; }
    public String getHomeAddress() { return homeAddress; }
    public UUID getDistrictId() { return districtId; }
    public UUID getRiverBasinId() { return riverBasinId; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public UUID getOrganisationId() { return organisationId; }
    public UUID getRescueTeamId() { return rescueTeamId; }
}
""",
    "DistrictView.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for District. */
public record DistrictView(UUID id, String code, String name, String province) {
    public DistrictView(District d) {
        this(d.getId(), d.getCode(), d.getName(), d.getProvince());
    }
}
""",
    "RiverBasinView.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for RiverBasin. */
public record RiverBasinView(UUID id, String code, String name) {
    public RiverBasinView(RiverBasin b) {
        this(b.getId(), b.getCode(), b.getName());
    }
}
""",
    "HazardTypeView.java": """package lk.dmc.disaster.shared.reference;
import java.util.List;
import java.util.UUID;
/** View for HazardType. */
public record HazardTypeView(UUID id, String code, String name, OnsetSpeed onsetSpeed, List<String> reportCategories, boolean active) {
    public HazardTypeView(HazardType h) {
        this(h.getId(), h.getCode(), h.getName(), h.getOnsetSpeed(), h.getReportCategories(), h.isActive());
    }
}
""",
    "OrganisationView.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for Organisation. */
public record OrganisationView(UUID id, String name, OrganisationType type, String contactPhone) {
    public OrganisationView(Organisation o) {
        this(o.getId(), o.getName(), o.getType(), o.getContactPhone());
    }
}
""",
    "ReliefItemView.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** View for ReliefItem. */
public record ReliefItemView(UUID id, String code, String name, String unit, ReliefCategory category) {
    public ReliefItemView(ReliefItem r) {
        this(r.getId(), r.getCode(), r.getName(), r.getUnit(), r.getCategory());
    }
}
""",
    "DisasterEventView.java": """package lk.dmc.disaster.shared.reference;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/** View for DisasterEvent. */
public record DisasterEventView(UUID id, String name, UUID hazardTypeId, EventStatus status, OffsetDateTime startedAt, OffsetDateTime endedAt, Set<UUID> districtIds) {
    public DisasterEventView(DisasterEvent e) {
        this(e.getId(), e.getName(), e.getHazardTypeId(), e.getStatus(), e.getStartedAt(), e.getEndedAt(), e.getDistricts().stream().map(District::getId).collect(Collectors.toSet()));
    }
}
""",
    "UserView.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
/** View for User. */
public record UserView(UUID id, String fullName, Role role, UUID districtId, UUID riverBasinId, UUID organisationId, UUID rescueTeamId) {
    public UserView(AppUser u) {
        this(u.getId(), u.getFullName(), u.getRole(), u.getDistrictId(), u.getRiverBasinId(), u.getOrganisationId(), u.getRescueTeamId());
    }
}
""",
    "CitizenContact.java": """package lk.dmc.disaster.shared.reference;
import java.util.UUID;
/** Citizen contact info. */
public record CitizenContact(UUID userId, String phone, String preferredLanguage, UUID districtId, UUID riverBasinId) {
    public CitizenContact(AppUser u) {
        this(u.getId(), u.getPhone(), u.getPreferredLanguage(), u.getDistrictId(), u.getRiverBasinId());
    }
}
"""
}

for name, content in files.items():
    with open(os.path.join(base_dir, name), 'w') as f:
        f.write(content)

print("Generated core entities and records.")
