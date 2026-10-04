package lk.dmc.disaster.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * The login view of a person in the {@code users} table. Holds credentials and the rules that
 * change them (lockout, verification); everything else about a user belongs to other modules.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  private String phone;
  private String email;
  private String nic;

  @Column(name = "home_address")
  private String homeAddress;

  @Column(name = "district_id", nullable = false)
  private UUID districtId;

  @Column(name = "river_basin_id")
  private UUID riverBasinId;

  @Column(name = "preferred_language", nullable = false)
  private String preferredLanguage;

  @Column(name = "rescue_team_id")
  private UUID rescueTeamId;

  @Column(name = "password_hash")
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status;

  @Column(name = "phone_verified", nullable = false)
  private boolean phoneVerified;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** A self-registered citizen: inactive until the mobile number is verified. */
  public static UserAccount newCitizen(
      String fullName,
      PhoneNumber phone,
      String email,
      String nic,
      String homeAddress,
      UUID districtId,
      UUID riverBasinId,
      String preferredLanguage,
      String passwordHash) {
    UserAccount u = new UserAccount();
    u.id = UUID.randomUUID();
    u.role = Role.CITIZEN;
    u.fullName = fullName;
    u.phone = phone.e164();
    u.email = email;
    u.nic = nic;
    u.homeAddress = homeAddress;
    u.districtId = districtId;
    u.riverBasinId = riverBasinId;
    u.preferredLanguage = preferredLanguage;
    u.passwordHash = passwordHash;
    u.status = AccountStatus.PENDING_VERIFICATION;
    u.phoneVerified = false;
    return u;
  }

  public boolean isLocked(Instant now) {
    return lockedUntil != null && lockedUntil.isAfter(now);
  }

  public boolean awaitsPhoneVerification() {
    return status == AccountStatus.PENDING_VERIFICATION;
  }

  public boolean canLogIn() {
    return status != AccountStatus.DISABLED;
  }

  /** Counts a failed login; at the limit the account locks and the counter restarts. */
  public void recordFailedLogin(int maxFailures, Instant lockUntil) {
    failedLoginAttempts++;
    if (failedLoginAttempts >= maxFailures) {
      lockedUntil = lockUntil;
      failedLoginAttempts = 0;
    }
  }

  public void clearLoginFailures() {
    failedLoginAttempts = 0;
    lockedUntil = null;
  }

  public void markPhoneVerified() {
    phoneVerified = true;
    status = AccountStatus.ACTIVE;
  }
}
