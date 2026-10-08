package lk.dmc.disaster.reports;

import java.time.Instant;
import java.util.UUID;

/**
 * Published inside the verify transaction. The warnings module links the report to a hazard.
 * Coordinates are null for a report with a described place and no GPS. {@code severity} is how
 * dangerous the verifying officer judged the hazard (1 to 5), or null when none was given.
 */
public record ReportVerifiedEvent(
    UUID reportId,
    UUID hazardTypeId,
    String category,
    UUID districtId,
    Double latitude,
    Double longitude,
    Instant verifiedAt,
    Integer severity) {

  /** A verification where the officer gave no severity. */
  public ReportVerifiedEvent(
      UUID reportId,
      UUID hazardTypeId,
      String category,
      UUID districtId,
      Double latitude,
      Double longitude,
      Instant verifiedAt) {
    this(reportId, hazardTypeId, category, districtId, latitude, longitude, verifiedAt, null);
  }
}
