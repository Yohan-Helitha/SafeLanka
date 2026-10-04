package lk.dmc.disaster.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A one-time SMS code. Only its SHA-256 hash is stored. */
@Entity
@Table(name = "phone_verification_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerificationCode {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "code_hash", nullable = false)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public static PhoneVerificationCode issue(
      UUID userId, String codeHash, Instant now, Instant expiresAt) {
    PhoneVerificationCode c = new PhoneVerificationCode();
    c.id = UUID.randomUUID();
    c.userId = userId;
    c.codeHash = codeHash;
    c.createdAt = now;
    c.expiresAt = expiresAt;
    return c;
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public boolean hasAttemptsLeft(int maxAttempts) {
    return attempts < maxAttempts;
  }

  public void registerFailedAttempt() {
    attempts++;
  }

  public void consume(Instant now) {
    consumedAt = now;
  }
}
