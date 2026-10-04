package lk.dmc.disaster.shared.actor;

import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;

/** The authenticated person behind the current request, as modules see them. */
public record ActingUser(
    UUID id, Role role, UUID districtId, UUID riverBasinId, UUID rescueTeamId) {

  public boolean hasRole(Role... roles) {
    for (Role r : roles) {
      if (r == role) {
        return true;
      }
    }
    return false;
  }
}
