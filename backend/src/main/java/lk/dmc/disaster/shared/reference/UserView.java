package lk.dmc.disaster.shared.reference;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
/** View for User. */
public record UserView(UUID id, String fullName, Role role, UUID districtId, UUID riverBasinId, UUID organisationId, UUID rescueTeamId) {
    public UserView(AppUser u) {
        this(u.getId(), u.getFullName(), u.getRole(), u.getDistrictId(), u.getRiverBasinId(), u.getOrganisationId(), u.getRescueTeamId());
    }
}
