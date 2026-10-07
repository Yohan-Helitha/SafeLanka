package lk.dmc.disaster.reports.entity;

/** Where a ground report is in its review. Values match the CHECK on {@code hazard_reports}. */
public enum ReportStatus {
  PENDING,
  NEEDS_MORE_INFO,
  VERIFIED,
  REJECTED;

  /** Verified and rejected reports cannot be decided again. */
  public boolean isFinal() {
    return this == VERIFIED || this == REJECTED;
  }
}
