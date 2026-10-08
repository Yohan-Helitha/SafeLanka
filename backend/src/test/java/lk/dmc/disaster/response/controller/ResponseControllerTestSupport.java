package lk.dmc.disaster.response.controller;

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

abstract class ResponseControllerTestSupport {

  static final UUID DISTRICT = UUID.randomUUID();

  @Mock protected ActingUserContext actingUser;

  protected MockMvc mvcFor(Object controller) {
    return MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .addInterceptors(new RoleInterceptor(actingUser))
        .build();
  }

  protected ActingUser signedInAs(Role role) {
    ActingUser user = new ActingUser(UUID.randomUUID(), role, DISTRICT, null, null);
    when(actingUser.require()).thenReturn(user);
    return user;
  }

  protected ActingUser signedInAs(Role role, UUID districtId, UUID rescueTeamId) {
    ActingUser user = new ActingUser(UUID.randomUUID(), role, districtId, null, rescueTeamId);
    when(actingUser.require()).thenReturn(user);
    return user;
  }
}
