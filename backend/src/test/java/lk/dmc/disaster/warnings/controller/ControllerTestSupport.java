package lk.dmc.disaster.warnings.controller;

import static org.mockito.Mockito.when;

import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.actor.RoleInterceptor;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.error.GlobalExceptionHandler;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Runs a controller with the real error envelope and the real role check, but no Spring context and
 * no database.
 */
abstract class ControllerTestSupport {

  static final UUID DISTRICT = UUID.randomUUID();
  static final UUID BASIN = UUID.randomUUID();

  @Mock protected ActingUserContext actingUser;

  protected MockMvc mvcFor(Object controller) {
    return MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .addInterceptors(new RoleInterceptor(actingUser))
        .build();
  }

  protected ActingUser signedInAs(Role role) {
    ActingUser user = new ActingUser(UUID.randomUUID(), role, DISTRICT, BASIN, null);
    when(actingUser.require()).thenReturn(user);
    return user;
  }
}
