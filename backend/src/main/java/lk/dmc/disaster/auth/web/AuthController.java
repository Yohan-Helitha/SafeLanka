package lk.dmc.disaster.auth.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lk.dmc.disaster.auth.application.AuthSession;
import lk.dmc.disaster.auth.application.LoginService;
import lk.dmc.disaster.auth.application.PhoneVerificationService;
import lk.dmc.disaster.auth.application.ProfileService;
import lk.dmc.disaster.auth.application.SessionService;
import lk.dmc.disaster.auth.application.SignupService;
import lk.dmc.disaster.auth.application.UserProfile;
import lk.dmc.disaster.auth.web.AuthRequests.LoginRequest;
import lk.dmc.disaster.auth.web.AuthRequests.ResendCodeRequest;
import lk.dmc.disaster.auth.web.AuthRequests.VerifyPhoneRequest;
import lk.dmc.disaster.auth.web.AuthResponses.LoginResponse;
import lk.dmc.disaster.auth.web.AuthResponses.OtpResponse;
import lk.dmc.disaster.auth.web.AuthResponses.SignupResponse;
import lk.dmc.disaster.shared.actor.ActingUserContext;
import lk.dmc.disaster.shared.api.ApiResponse;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter only: validates input, calls the services, and handles the refresh cookie. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

  private final SignupService signup;
  private final PhoneVerificationService verification;
  private final LoginService login;
  private final SessionService sessions;
  private final ProfileService profiles;
  private final ActingUserContext actingUser;
  private final RefreshCookies cookies;

  AuthController(
      SignupService signup,
      PhoneVerificationService verification,
      LoginService login,
      SessionService sessions,
      ProfileService profiles,
      ActingUserContext actingUser,
      RefreshCookies cookies) {
    this.signup = signup;
    this.verification = verification;
    this.login = login;
    this.sessions = sessions;
    this.profiles = profiles;
    this.actingUser = actingUser;
    this.cookies = cookies;
  }

  @PostMapping("/signup")
  ResponseEntity<ApiResponse<SignupResponse>> signUp(@Valid @RequestBody SignupRequest request) {
    SignupResponse body = SignupResponse.from(signup.signUp(request.toCommand()));
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(body));
  }

  @PostMapping("/verify-phone")
  ApiResponse<LoginResponse> verifyPhone(
      @Valid @RequestBody VerifyPhoneRequest request, HttpServletResponse response) {
    return signedIn(verification.verifyAndSignIn(request.phone(), request.code()), response);
  }

  @PostMapping("/resend-code")
  ResponseEntity<ApiResponse<OtpResponse>> resendCode(
      @Valid @RequestBody ResendCodeRequest request) {
    OtpResponse body = OtpResponse.from(verification.resend(request.phone()));
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(body));
  }

  @PostMapping("/login")
  ApiResponse<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    return signedIn(login.login(request.toCommand()), response);
  }

  @PostMapping("/refresh")
  ApiResponse<LoginResponse> refresh(
      @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED, "Log in to continue.");
    }
    try {
      return signedIn(sessions.refresh(refreshToken), response);
    } catch (AppException e) {
      cookies.clear(response); // a dead cookie is no use to the browser
      throw e;
    }
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(
      @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    if (refreshToken != null && !refreshToken.isBlank()) {
      sessions.logout(refreshToken);
    }
    cookies.clear(response);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  ApiResponse<UserProfile> me() {
    return ApiResponse.of(profiles.me(actingUser.require().id()));
  }

  private ApiResponse<LoginResponse> signedIn(AuthSession session, HttpServletResponse response) {
    cookies.write(response, session);
    return ApiResponse.of(LoginResponse.from(session));
  }
}
