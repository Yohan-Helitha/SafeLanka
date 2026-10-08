package lk.dmc.disaster.warnings.service;

import java.util.UUID;

/**
 * What the warnings module needs to know about a newly verified report. {@code severity} is how
 * dangerous the verifying officer judged the hazard (1 to 5), or null when none was given.
 */
public record VerifiedReportRef(
    UUID reportId, UUID hazardTypeId, UUID districtId, String description, Integer severity) {

  /** A report verified without an officer's severity. */
  public VerifiedReportRef(
      UUID reportId, UUID hazardTypeId, UUID districtId, String description) {
    this(reportId, hazardTypeId, districtId, description, null);
  }
}
