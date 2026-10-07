package lk.dmc.disaster.shared.actor;

import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.error.NotFoundException;

/** Read-only view of users for modules that must show a person's name and role. */
public interface UserDirectory {

  Optional<UserSummary> find(UUID userId);

  default UserSummary require(UUID userId) {
    return find(userId).orElseThrow(() -> new NotFoundException("User not found."));
  }
}
