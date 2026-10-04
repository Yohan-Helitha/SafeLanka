package lk.dmc.disaster.auth.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.auth.domain.PhoneVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhoneVerificationCodeRepository
    extends JpaRepository<PhoneVerificationCode, UUID> {

  Optional<PhoneVerificationCode> findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
      UUID userId);

  Optional<PhoneVerificationCode> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

  /** A new code replaces any earlier one that was never used. */
  @Modifying
  @Query(
      "update PhoneVerificationCode c set c.consumedAt = :now"
          + " where c.userId = :userId and c.consumedAt is null")
  int consumeAllOpen(@Param("userId") UUID userId, @Param("now") Instant now);
}
