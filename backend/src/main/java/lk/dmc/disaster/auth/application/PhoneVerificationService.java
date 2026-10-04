package lk.dmc.disaster.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lk.dmc.disaster.auth.application.port.OtpCodeGenerator;
import lk.dmc.disaster.auth.config.AuthProperties;
import lk.dmc.disaster.auth.domain.PhoneNumber;
import lk.dmc.disaster.auth.domain.PhoneVerificationCode;
import lk.dmc.disaster.auth.domain.UserAccount;
import lk.dmc.disaster.auth.persistence.PhoneVerificationCodeRepository;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.sms.SmsDeliveryException;
import lk.dmc.disaster.shared.sms.SmsGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends and checks the 6-digit SMS code that activates a citizen account. */
@Slf4j
@Service
public class PhoneVerificationService {

  private static final String REQUEST_NEW_CODE = "Request a new code.";

  private final UserAccountRepository users;
  private final PhoneVerificationCodeRepository codes;
  private final SmsGateway sms;
  private final OtpCodeGenerator generator;
  private final SessionService sessions;
  private final AuthProperties props;
  private final Clock clock;

  PhoneVerificationService(
      UserAccountRepository users,
      PhoneVerificationCodeRepository codes,
      SmsGateway sms,
      OtpCodeGenerator generator,
      SessionService sessions,
      AuthProperties props,
      Clock clock) {
    this.users = users;
    this.codes = codes;
    this.sms = sms;
    this.generator = generator;
    this.sessions = sessions;
    this.props = props;
    this.clock = clock;
  }

  /** Sends a new code. Fails with 429 inside the resend cooldown. */
  @Transactional
  public OtpReceipt issueFor(UserAccount user) {
    Optional<Duration> wait = cooldownRemaining(user);
    if (wait.isPresent()) {
      long seconds = Math.max(1, wait.get().toSeconds());
      throw new AppException(
          ErrorCode.TOO_MANY_REQUESTS,
          "Wait " + seconds + " seconds before asking for another code.",
          Map.of("retryAfterSeconds", seconds));
    }
    return send(user);
  }

  /** Used at login: sends a code unless one was sent a moment ago, then reports its expiry. */
  @Transactional
  public OtpReceipt ensureCodeSent(UserAccount user) {
    if (cooldownRemaining(user).isPresent()) {
      Instant expiresAt =
          codes
              .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
              .map(PhoneVerificationCode::getExpiresAt)
              .orElse(clock.instant().plus(props.otpTtl()));
      return new OtpReceipt(expiresAt, null);
    }
    return send(user);
  }

  /**
   * Resend by phone number. Unknown or already verified numbers get the same answer, sending
   * nothing.
   */
  @Transactional
  public OtpReceipt resend(String rawPhone) {
    Optional<UserAccount> user =
        PhoneNumber.parse(rawPhone)
            .flatMap(p -> users.findByPhone(p.e164()))
            .filter(UserAccount::awaitsPhoneVerification);
    if (user.isEmpty()) {
      return new OtpReceipt(clock.instant().plus(props.otpTtl()), null);
    }
    return issueFor(user.get());
  }

  /**
   * Checks the code and, when right, activates the account and signs the person in. A wrong guess
   * is counted and must persist even though the request fails, hence {@code noRollbackFor}.
   */
  @Transactional(noRollbackFor = AppException.class)
  public AuthSession verifyAndSignIn(String rawPhone, String code) {
    UserAccount user =
        PhoneNumber.parse(rawPhone)
            .flatMap(p -> users.findByPhone(p.e164()))
            .filter(UserAccount::awaitsPhoneVerification)
            .flatMap(u -> users.findByIdForUpdate(u.getId()))
            .orElseThrow(() -> new AppException(ErrorCode.CODE_EXPIRED, REQUEST_NEW_CODE));

    Instant now = clock.instant();
    PhoneVerificationCode current =
        codes
            .findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
            .filter(c -> !c.isExpired(now) && c.hasAttemptsLeft(props.otpMaxAttempts()))
            .orElseThrow(() -> new AppException(ErrorCode.CODE_EXPIRED, REQUEST_NEW_CODE));

    if (!matches(code, current.getCodeHash())) {
      current.registerFailedAttempt();
      int left = props.otpMaxAttempts() - current.getAttempts();
      throw new AppException(
          ErrorCode.CODE_INVALID,
          left > 0
              ? "That code is incorrect. " + left + (left == 1 ? " try left." : " tries left.")
              : "That code is incorrect. " + REQUEST_NEW_CODE,
          Map.of("attemptsLeft", left));
    }

    current.consume(now);
    user.markPhoneVerified();
    return sessions.start(user);
  }

  private OtpReceipt send(UserAccount user) {
    Instant now = clock.instant();
    codes.consumeAllOpen(user.getId(), now);
    String code = generator.next();
    Instant expiresAt = now.plus(props.otpTtl());
    PhoneVerificationCode issued =
        codes.save(
            PhoneVerificationCode.issue(user.getId(), TokenHasher.sha256(code), now, expiresAt));
    try {
      sms.send(
          user.getPhone(),
          "SafeLanka: your verification code is "
              + code
              + ". It expires in "
              + props.otpTtl().toMinutes()
              + " minutes.");
    } catch (SmsDeliveryException e) {
      // Nothing reached the phone, so the code must not start the resend cooldown or stay usable.
      codes.delete(issued);
      log.warn("Verification SMS could not be sent: {}", e.getMessage());
      throw new AppException(
          ErrorCode.SMS_UNAVAILABLE,
          "We could not send the code right now. Try again in a minute.");
    }
    // A code may be echoed to the screen only while SMS is simulated, never with a real gateway.
    return new OtpReceipt(expiresAt, props.exposeDevCode() && sms.simulated() ? code : null);
  }

  private Optional<Duration> cooldownRemaining(UserAccount user) {
    Instant now = clock.instant();
    return codes
        .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
        .map(c -> Duration.between(now, c.getCreatedAt().plus(props.otpResendCooldown())))
        .filter(d -> !d.isNegative() && !d.isZero());
  }

  private static boolean matches(String submitted, String storedHash) {
    // Constant-time comparison of the two hashes.
    return MessageDigest.isEqual(
        TokenHasher.sha256(submitted.trim()).getBytes(StandardCharsets.UTF_8),
        storedHash.getBytes(StandardCharsets.UTF_8));
  }
}
