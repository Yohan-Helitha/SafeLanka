package lk.dmc.disaster.auth.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.auth.config.AuthProperties;
import lk.dmc.disaster.auth.domain.RefreshToken;
import lk.dmc.disaster.auth.persistence.RefreshTokenRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues, rotates and revokes refresh tokens. Only hashes are stored. */
@Service
public class RefreshTokenService {

  public record Issued(String rawToken, Instant expiresAt) {}

  public record Rotated(UUID userId, Issued next) {}

  private final RefreshTokenRepository tokens;
  private final AuthProperties props;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public RefreshTokenService(RefreshTokenRepository tokens, AuthProperties props, Clock clock) {
    this.tokens = tokens;
    this.props = props;
    this.clock = clock;
  }

  @Transactional
  public Issued issue(UUID userId) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant now = clock.instant();
    Instant expiresAt = now.plus(props.refreshTokenTtl());
    tokens.save(RefreshToken.issue(userId, TokenHasher.sha256(raw), now, expiresAt));
    return new Issued(raw, expiresAt);
  }

  /**
   * Swaps a valid token for a new one. Presenting a token that was already rotated means it may
   * have been stolen, so every token of that user is revoked; that revocation must commit even
   * though the request fails, hence {@code noRollbackFor}.
   */
  @Transactional(noRollbackFor = AppException.class)
  public Rotated rotate(String rawToken) {
    Instant now = clock.instant();
    RefreshToken current =
        tokens.findByTokenHash(TokenHasher.sha256(rawToken)).orElseThrow(this::sessionEnded);
    if (current.isRevoked()) {
      tokens.revokeAllForUser(current.getUserId(), now);
      throw sessionEnded();
    }
    if (current.isExpired(now)) {
      throw sessionEnded();
    }
    current.revoke(now);
    return new Rotated(current.getUserId(), issue(current.getUserId()));
  }

  @Transactional
  public void revoke(String rawToken) {
    tokens.findByTokenHash(TokenHasher.sha256(rawToken)).ifPresent(t -> t.revoke(clock.instant()));
  }

  private AppException sessionEnded() {
    return new AppException(
        ErrorCode.UNAUTHENTICATED, "Your session ended. Log in again.", Map.of());
  }
}
