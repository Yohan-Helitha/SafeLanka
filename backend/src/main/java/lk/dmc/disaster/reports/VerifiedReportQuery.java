package lk.dmc.disaster.reports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What the warnings module may ask the reports module: verified evidence only. */
public interface VerifiedReportQuery {

  /** VERIFIED reports only, newest verification first; null filter fields mean "any"; max 200. */
  List<VerifiedReportSummary> findVerified(VerifiedReportFilter filter);

  /** Empty when the report does not exist or is not VERIFIED. */
  Optional<VerifiedReportSummary> findVerifiedById(UUID reportId);
}
