package lk.dmc.disaster.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** All tunable authentication rules, bound from {@code app.auth.*}. */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    @DefaultValue("safelanka") String issuer,
    String jwtSecret,
    @DefaultValue("15m") Duration accessTokenTtl,
    @DefaultValue("7d") Duration refreshTokenTtl,
    @DefaultValue("5m") Duration otpTtl,
    @DefaultValue("3") int otpMaxAttempts,
    @DefaultValue("60s") Duration otpResendCooldown,
    @DefaultValue("5") int maxFailedLogins,
    @DefaultValue("15m") Duration lockDuration,
    @DefaultValue("true") boolean cookieSecure,
    @DefaultValue("false") boolean exposeDevCode) {

  private static final int MIN_SECRET_BYTES = 32;

  public AuthProperties {
    if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "app.auth.jwt-secret (environment variable JWT_SECRET) must be set to at least "
              + MIN_SECRET_BYTES
              + " characters. See backend/.env.example.");
    }
  }
}
