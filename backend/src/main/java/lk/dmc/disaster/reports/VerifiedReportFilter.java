package lk.dmc.disaster.reports;

import java.time.Instant;
import java.util.UUID;

/** Optional narrowing for {@link VerifiedReportQuery#findVerified}; {@code since} is inclusive. */
public record VerifiedReportFilter(UUID hazardTypeId, UUID districtId, Instant since) {

  /** A filter that matches every verified report. */
  public static VerifiedReportFilter any() {
    return new VerifiedReportFilter(null, null, null);
  }
}
