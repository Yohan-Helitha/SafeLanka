package lk.dmc.disaster.auth.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.auth.application.AuthSession;
import lk.dmc.disaster.auth.application.OtpReceipt;
import lk.dmc.disaster.auth.application.SignupService.SignupReceipt;
import lk.dmc.disaster.auth.application.UserProfile;

/** Response bodies. The refresh token never appears here: it travels only in the cookie. */
final class AuthResponses {

  private AuthResponses() {}

  record LoginResponse(String accessToken, long expiresIn, UserProfile user) {

    static LoginResponse from(AuthSession s) {
      return new LoginResponse(s.accessToken(), s.accessTokenValidFor().toSeconds(), s.user());
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record OtpResponse(Instant otpExpiresAt, String devCode) {

    static OtpResponse from(OtpReceipt r) {
      return new OtpResponse(r.expiresAt(), r.devCode());
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record SignupResponse(UUID userId, String phone, Instant otpExpiresAt, String devCode) {

    static SignupResponse from(SignupReceipt r) {
      return new SignupResponse(
          r.userId(), r.maskedPhone(), r.otp().expiresAt(), r.otp().devCode());
    }
  }
}
