package lk.dmc.disaster.shared.actor;

import jakarta.servlet.http.HttpServletRequest;

/** Write side: authentication filters register who the request acts as. */
public interface ActingUserRegistrar {

  void register(HttpServletRequest request, ActingUser user);
}
