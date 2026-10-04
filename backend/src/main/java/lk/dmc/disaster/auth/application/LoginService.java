package lk.dmc.disaster.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lk.dmc.disaster.auth.config.AuthProperties;
import lk.dmc.disaster.auth.domain.PhoneNumber;
import lk.dmc.disaster.auth.domain.UserAccount;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checks credentials with lockout, then starts a session. */
@Service
public class LoginService {

  public enum IdentifierType {
    PHONE,
    EMAIL
  }

  public record LoginCommand(IdentifierType type, String identifier, String password) {}

  private static final DateTimeFormatter CLOCK_TIME =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Colombo"));

  private final UserAccountRepository users;
  private final PasswordEncoder passwordEncoder;
  private final PhoneVerificationService verification;
  private final SessionService sessions;
  private final AuthProperties props;
  private final Clock clock;

  /**
   * A real hash to compare against when the account does not exist, so timing does not give it
   * away.
   */
  private final String decoyHash;

  LoginService(
      UserAccountRepository users,
      PasswordEncoder passwordEncoder,
      PhoneVerificationService verification,
      SessionService sessions,
      AuthProperties props,
      Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.verification = verification;
    this.sessions = sessions;
    this.props = props;
    this.clock = clock;
    this.decoyHash = passwordEncoder.encode("decoy-password-for-timing");
  }

  /**
   * Failed attempts are counted and the lock is stored even though the request fails, hence {@code
   * noRollbackFor}. The row lock serialises concurrent attempts on one account.
   */
  @Transactional(noRollbackFor = AppException.class)
  public AuthSession login(LoginCommand command) {
    Optional<UserAccount> found = find(command).flatMap(u -> users.findByIdForUpdate(u.getId()));
    if (found.isEmpty()) {
      passwordEncoder.matches(command.password(), decoyHash);
      throw invalidCredentials(command.type());
    }
    UserAccount user = found.get();
    Instant now = clock.instant();

    if (user.isLocked(now)) {
      throw locked(user.getLockedUntil());
    }
    boolean passwordOk =
        user.getPasswordHash() != null
            && passwordEncoder.matches(command.password(), user.getPasswordHash());
    if (!passwordOk || !user.canLogIn()) {
      user.recordFailedLogin(props.maxFailedLogins(), now.plus(props.lockDuration()));
      if (user.isLocked(now)) {
        throw locked(user.getLockedUntil());
      }
      throw invalidCredentials(command.type());
    }

    user.clearLoginFailures();
    if (user.awaitsPhoneVerification()) {
      OtpReceipt receipt = verification.ensureCodeSent(user);
      Map<String, Object> details = new HashMap<>();
      details.put("phone", user.getPhone());
      details.put("otpExpiresAt", receipt.expiresAt().toString());
      if (receipt.devCode() != null) {
        details.put("devCode", receipt.devCode());
      }
      throw new AppException(
          ErrorCode.PHONE_NOT_VERIFIED,
          "Verify your mobile number to continue. We sent you a new code.",
          details);
    }
    return sessions.start(user);
  }

  private Optional<UserAccount> find(LoginCommand command) {
    String identifier = command.identifier() == null ? "" : command.identifier().trim();
    return switch (command.type()) {
      case PHONE -> PhoneNumber.parse(identifier).flatMap(p -> users.findByPhone(p.e164()));
      case EMAIL -> users.findByEmail(identifier.toLowerCase(Locale.ROOT));
    };
  }

  /** One message for every credential failure, so it never reveals which part was wrong. */
  private static AppException invalidCredentials(IdentifierType type) {
    String what = type == IdentifierType.PHONE ? "mobile number" : "email";
    return new AppException(
        ErrorCode.INVALID_CREDENTIALS, "The " + what + " or password is incorrect.");
  }

  private static AppException locked(Instant until) {
    return new AppException(
        ErrorCode.ACCOUNT_LOCKED,
        "Too many attempts. Try again after " + CLOCK_TIME.format(until) + ".",
        Map.of("lockedUntil", until.toString()));
  }
}
