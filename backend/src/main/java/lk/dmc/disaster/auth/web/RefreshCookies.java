package lk.dmc.disaster.auth.web;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import lk.dmc.disaster.auth.application.AuthSession;
import lk.dmc.disaster.auth.config.AuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Reads the cookie name and flags in one place: httpOnly, SameSite=Strict, limited to /api/auth.
 */
@Component
class RefreshCookies {

  static final String NAME = "refresh_token";
  private static final String PATH = "/api/auth";

  private final AuthProperties props;
  private final Clock clock;

  RefreshCookies(AuthProperties props, Clock clock) {
    this.props = props;
    this.clock = clock;
  }

  void write(HttpServletResponse response, AuthSession session) {
    Duration maxAge = Duration.between(clock.instant(), session.refreshTokenExpiresAt());
    add(response, build(session.refreshToken(), maxAge));
  }

  void clear(HttpServletResponse response) {
    add(response, build("", Duration.ZERO));
  }

  private ResponseCookie build(String value, Duration maxAge) {
    return ResponseCookie.from(NAME, value)
        .httpOnly(true)
        .secure(props.cookieSecure())
        .sameSite("Strict")
        .path(PATH)
        .maxAge(maxAge)
        .build();
  }

  private static void add(HttpServletResponse response, ResponseCookie cookie) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
