package lk.dmc.disaster.shared.domain;

/** Roles a user can hold. Values match the CHECK constraint on {@code users.role}. */
public enum Role {
  CITIZEN,
  VOLUNTEER,
  DMC_OFFICER,
  DISTRICT_OFFICER,
  RESCUE_MEMBER,
  SHELTER_COORDINATOR
}
