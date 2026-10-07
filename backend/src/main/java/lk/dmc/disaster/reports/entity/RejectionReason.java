package lk.dmc.disaster.reports.entity;

/** Why an officer rejected a report. Values match the CHECK on {@code hazard_reports}. */
public enum RejectionReason {
  INSUFFICIENT_EVIDENCE,
  DUPLICATE,
  LOCATION_MISMATCH,
  NOT_A_HAZARD,
  OTHER
}
