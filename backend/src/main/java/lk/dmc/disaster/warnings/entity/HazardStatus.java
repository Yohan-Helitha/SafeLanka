package lk.dmc.disaster.warnings.entity;

/** Assessment lifecycle of a hazard. Values match the CHECK on hazards.status. */
public enum HazardStatus {
  UNDER_ASSESSMENT,
  WARNED,
  MONITORING,
  RESOLVED
}
