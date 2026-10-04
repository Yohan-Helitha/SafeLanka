package lk.dmc.disaster.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lk.dmc.disaster.auth.application.LoginService.IdentifierType;
import lk.dmc.disaster.auth.application.LoginService.LoginCommand;

/** Request bodies for the small auth endpoints. */
final class AuthRequests {

  private AuthRequests() {}

  record LoginRequest(
      @NotNull(message = "Choose mobile number or email.") IdentifierType identifierType,
      @NotBlank(message = "Enter your mobile number or email.") String identifier,
      @NotBlank(message = "Enter your password.") String password) {

    LoginCommand toCommand() {
      return new LoginCommand(identifierType, identifier, password);
    }
  }

  record VerifyPhoneRequest(
      @NotBlank(message = "Enter your mobile number.") String phone,
      @NotBlank(message = "Enter the 6-digit code.")
          @Pattern(regexp = "^\\d{6}$", message = "The code has 6 digits.")
          String code) {}

  record ResendCodeRequest(@NotBlank(message = "Enter your mobile number.") String phone) {}
}
