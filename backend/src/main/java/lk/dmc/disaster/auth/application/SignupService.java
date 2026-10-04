package lk.dmc.disaster.auth.application;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lk.dmc.disaster.auth.application.port.DistrictDirectory;
import lk.dmc.disaster.auth.domain.Nic;
import lk.dmc.disaster.auth.domain.PhoneNumber;
import lk.dmc.disaster.auth.domain.UserAccount;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates a citizen account that stays inactive until the mobile number is verified. */
@Service
public class SignupService {

  public record SignupCommand(
      String fullName,
      String phone,
      String email,
      String nic,
      String homeAddress,
      UUID districtId,
      String preferredLanguage,
      String password) {}

  public record SignupReceipt(UUID userId, String maskedPhone, OtpReceipt otp) {}

  private final UserAccountRepository users;
  private final DistrictDirectory districts;
  private final PasswordEncoder passwordEncoder;
  private final PhoneVerificationService verification;

  SignupService(
      UserAccountRepository users,
      DistrictDirectory districts,
      PasswordEncoder passwordEncoder,
      PhoneVerificationService verification) {
    this.users = users;
    this.districts = districts;
    this.passwordEncoder = passwordEncoder;
    this.verification = verification;
  }

  @Transactional
  public SignupReceipt signUp(SignupCommand command) {
    PhoneNumber phone =
        PhoneNumber.parse(command.phone())
            .orElseThrow(() -> fieldError("phone", "Enter a Sri Lankan mobile number."));
    String email = normaliseEmail(command.email());
    String nic = Nic.normalise(command.nic());

    if (users.existsByPhone(phone.e164())) {
      throw conflict("phone", "That mobile number is already registered.");
    }
    if (email != null && users.existsByEmail(email)) {
      throw conflict("email", "That email is already registered.");
    }
    if (users.existsByNic(nic)) {
      throw conflict("nic", "That NIC is already registered.");
    }
    if (!districts.exists(command.districtId())) {
      throw fieldError("districtId", "Choose a district from the list.");
    }

    UserAccount account =
        UserAccount.newCitizen(
            command.fullName().trim(),
            phone,
            email,
            nic,
            command.homeAddress().trim(),
            command.districtId(),
            districts.riverBasinOf(command.districtId()).orElse(null),
            command.preferredLanguage(),
            passwordEncoder.encode(command.password()));
    try {
      users.saveAndFlush(account);
    } catch (DataIntegrityViolationException e) {
      // Two sign-ups raced past the checks above; the unique constraints are the final guard.
      throw conflict("phone", "That mobile number, email or NIC is already registered.");
    }
    return new SignupReceipt(account.getId(), phone.masked(), verification.issueFor(account));
  }

  private static String normaliseEmail(String email) {
    return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  private static AppException conflict(String field, String message) {
    return new AppException(ErrorCode.CONFLICT, message, Map.of("field", field));
  }

  private static AppException fieldError(String field, String message) {
    return new AppException(
        ErrorCode.VALIDATION_ERROR, message, Map.of("fields", Map.of(field, message)));
  }
}
