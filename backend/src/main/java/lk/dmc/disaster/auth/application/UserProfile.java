package lk.dmc.disaster.auth.application;

import java.util.UUID;
import lk.dmc.disaster.auth.domain.UserAccount;
import lk.dmc.disaster.shared.domain.Role;

/** What a client may know about the signed-in person. */
public record UserProfile(
    UUID id, String fullName, Role role, UUID districtId, UUID riverBasinId, UUID rescueTeamId) {

  static UserProfile from(UserAccount u) {
    return new UserProfile(
        u.getId(),
        u.getFullName(),
        u.getRole(),
        u.getDistrictId(),
        u.getRiverBasinId(),
        u.getRescueTeamId());
  }
}
