package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The warnings module's view of the reports module: which reports have been verified. A port, so
 * the real reports query can replace the SQL adapter without touching the warning rules.
 */
public interface VerifiedReports {

  /** The given reports that are VERIFIED. Ids of reports that are not verified are left out. */
  List<VerifiedReportSummary> findVerified(Collection<UUID> reportIds);
}
