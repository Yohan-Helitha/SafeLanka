package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Column;
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

    @Column(name = "full_name")
    private String fullName;
    private String phone;
    private String nic;
    @Column(name = "home_address")
    private String homeAddress;
    
    @Column(name = "district_id")
    private UUID districtId;
    @Column(name = "river_basin_id")
    private UUID riverBasinId;
    @Column(name = "preferred_language")
    private String preferredLanguage;
    @Column(name = "organisation_id")
    private UUID organisationId;
    @Column(name = "rescue_team_id")
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
