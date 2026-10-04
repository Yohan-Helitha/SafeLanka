package lk.dmc.disaster.auth.security;

import java.util.UUID;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Names of the custom claims in the access token, and the read side that rebuilds an ActingUser.
 */
final class JwtClaims {

  static final String ROLE = "role";
  static final String DISTRICT = "did";
  static final String RIVER_BASIN = "rbid";
  static final String RESCUE_TEAM = "tid";

  private JwtClaims() {}

  static ActingUser toActingUser(Jwt jwt) {
    return new ActingUser(
        UUID.fromString(jwt.getSubject()),
        Role.valueOf(jwt.getClaimAsString(ROLE)),
        uuid(jwt, DISTRICT),
        uuid(jwt, RIVER_BASIN),
        uuid(jwt, RESCUE_TEAM));
  }

  private static UUID uuid(Jwt jwt, String claim) {
    String value = jwt.getClaimAsString(claim);
    return value == null ? null : UUID.fromString(value);
  }
}
