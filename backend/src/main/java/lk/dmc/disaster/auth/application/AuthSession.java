package lk.dmc.disaster.auth.application;

import java.time.Duration;
import java.time.Instant;

/**
 * The result of a successful sign-in: the access token for the body, the refresh token for the
 * cookie.
 */
public record AuthSession(
    String accessToken,
    Duration accessTokenValidFor,
    UserProfile user,
    String refreshToken,
    Instant refreshTokenExpiresAt) {}
