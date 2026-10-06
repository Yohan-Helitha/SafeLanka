package lk.dmc.disaster.warnings.integration;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * The warnings module's view of the reports module: which reports have been verified. A port, so
 * the real reports query can replace the SQL adapter without touching the warning rules.
 */
public interface VerifiedReports {

  /** The subset of the given report ids whose status is VERIFIED. */
  Set<UUID> filterVerified(Collection<UUID> reportIds);
}
