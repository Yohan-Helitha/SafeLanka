package lk.dmc.disaster.auth.application;

import lk.dmc.disaster.auth.application.port.AccessTokenIssuer;
import lk.dmc.disaster.auth.application.port.AccessTokenIssuer.IssuedToken;
import lk.dmc.disaster.auth.domain.UserAccount;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Service;

/** Starts, renews and ends sessions: an access token plus a rotating refresh token. */
@Service
public class SessionService {

  private final AccessTokenIssuer accessTokens;
  private final RefreshTokenService refreshTokens;
  private final UserAccountRepository users;

  SessionService(
      AccessTokenIssuer accessTokens,
      RefreshTokenService refreshTokens,
      UserAccountRepository users) {
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
    this.users = users;
  }

  public AuthSession start(UserAccount user) {
    return build(user, refreshTokens.issue(user.getId()));
  }

  public AuthSession refresh(String rawRefreshToken) {
    RefreshTokenService.Rotated rotated = refreshTokens.rotate(rawRefreshToken);
    UserAccount user =
        users
            .findById(rotated.userId())
            .filter(UserAccount::canLogIn)
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.UNAUTHENTICATED, "Your session ended. Log in again."));
    return build(user, rotated.next());
  }

  public void logout(String rawRefreshToken) {
    refreshTokens.revoke(rawRefreshToken);
  }

  private AuthSession build(UserAccount user, RefreshTokenService.Issued refresh) {
    IssuedToken access = accessTokens.issue(user);
    return new AuthSession(
        access.value(),
        access.validFor(),
        UserProfile.from(user),
        refresh.rawToken(),
        refresh.expiresAt());
  }
}
