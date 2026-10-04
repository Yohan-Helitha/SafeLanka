package lk.dmc.disaster.auth.application.port;

import java.time.Duration;
import lk.dmc.disaster.auth.domain.UserAccount;

/** Issues the short-lived access token. JWT today; services depend only on this contract. */
public interface AccessTokenIssuer {

  IssuedToken issue(UserAccount user);

  record IssuedToken(String value, Duration validFor) {}
}
