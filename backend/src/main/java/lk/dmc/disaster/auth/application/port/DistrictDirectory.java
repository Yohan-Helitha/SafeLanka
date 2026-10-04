package lk.dmc.disaster.auth.application.port;

import java.util.Optional;
import java.util.UUID;

/** Reference-data lookups the sign-up needs, without depending on how they are stored. */
public interface DistrictDirectory {

  boolean exists(UUID districtId);

  /** The river basin a district drains to, which decides which warnings reach a citizen. */
  Optional<UUID> riverBasinOf(UUID districtId);
}
