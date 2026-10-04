package lk.dmc.disaster.auth.application;

import java.util.UUID;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private final UserAccountRepository users;

  ProfileService(UserAccountRepository users) {
    this.users = users;
  }

  @Transactional(readOnly = true)
  public UserProfile me(UUID userId) {
    return users
        .findById(userId)
        .map(UserProfile::from)
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED, "Log in to continue."));
  }
}
