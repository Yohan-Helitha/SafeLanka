package lk.dmc.disaster.auth.domain;

/** Lifecycle of an account. Values match the CHECK constraint on {@code users.status}. */
public enum AccountStatus {
  /** Signed up but the mobile number is not verified yet. */
  PENDING_VERIFICATION,
  ACTIVE,
  DISABLED
}
