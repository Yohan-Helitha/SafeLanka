package lk.dmc.disaster.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.auth.application.port.AccessTokenIssuer;
import lk.dmc.disaster.auth.config.AuthProperties;
import lk.dmc.disaster.auth.domain.UserAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtEncoder encoder;
  private final AuthProperties props;
  private final Clock clock;

  JwtAccessTokenIssuer(JwtEncoder encoder, AuthProperties props, Clock clock) {
    this.encoder = encoder;
    this.props = props;
    this.clock = clock;
  }

  @Override
  public IssuedToken issue(UserAccount user) {
    Instant now = clock.instant();
    JwtClaimsSet.Builder claims =
        JwtClaimsSet.builder()
            .issuer(props.issuer())
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(props.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .claim(JwtClaims.ROLE, user.getRole().name())
            .claim(JwtClaims.DISTRICT, user.getDistrictId().toString());
    if (user.getRiverBasinId() != null) {
      claims.claim(JwtClaims.RIVER_BASIN, user.getRiverBasinId().toString());
    }
    if (user.getRescueTeamId() != null) {
      claims.claim(JwtClaims.RESCUE_TEAM, user.getRescueTeamId().toString());
    }
    String token =
        encoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
            .getTokenValue();
    return new IssuedToken(token, props.accessTokenTtl());
  }
}
