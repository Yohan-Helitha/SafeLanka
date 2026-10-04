package lk.dmc.disaster.auth.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  Optional<UserAccount> findByPhone(String phone);

  Optional<UserAccount> findByEmail(String email);

  boolean existsByPhone(String phone);

  boolean existsByEmail(String email);

  boolean existsByNic(String nic);

  /** Row lock so two concurrent attempts cannot both read the same failure counter. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.id = :id")
  Optional<UserAccount> findByIdForUpdate(@Param("id") UUID id);
}
