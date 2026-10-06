package lk.dmc.disaster.warnings.entity;

/** Lifecycle of a public warning. Values match the CHECK on warnings.status. */
public enum WarningStatus {
  ACTIVE,
  ESCALATED,
  CANCELLED,
  EXPIRED
}
