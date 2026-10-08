package lk.dmc.disaster.warnings.entity;

/**
 * Lifecycle of a public warning. Values match the CHECK on warnings.status. ESCALATED is only found
 * on warnings replaced before an escalation began to raise the level of the same warning.
 */
public enum WarningStatus {
  ACTIVE,
  ESCALATED,
  CANCELLED,
  EXPIRED
}
