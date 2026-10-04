package lk.dmc.disaster.shared.actor;

import java.util.Optional;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;

/**
 * Read access to the acting user of the current request (interface segregation: no writer here).
 */
public interface ActingUserContext {

  Optional<ActingUser> current();

  /** The acting user, or a 401 when nobody is authenticated. */
  default ActingUser require() {
    return current()
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED, "Log in to continue."));
  }
}
