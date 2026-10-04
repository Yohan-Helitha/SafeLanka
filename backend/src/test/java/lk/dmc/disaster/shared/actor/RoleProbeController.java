package lk.dmc.disaster.shared.actor;

import lk.dmc.disaster.shared.domain.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints for {@link RequiresRoleTest}. They sit under {@code /api/auth/**}, which the
 * URL rules leave open, so only the role interceptor can reject a request to them.
 */
@RestController
class RoleProbeController {

  @GetMapping("/api/auth/probe-dmc")
  @RequiresRole(Role.DMC_OFFICER)
  String dmcOnly() {
    return "ok";
  }

  @GetMapping("/api/auth/probe-open")
  String open() {
    return "ok";
  }
}
