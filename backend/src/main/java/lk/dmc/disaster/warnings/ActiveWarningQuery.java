package lk.dmc.disaster.warnings;

import java.util.List;
import java.util.UUID;

/** Read-only view of live warnings, offered to other modules (for example the response module). */
public interface ActiveWarningQuery {

  /**
   * ACTIVE warnings that target the district directly or through a river basin containing it.
   * Highest level first, then newest first.
   */
  List<ActiveWarningSummary> findActiveForDistrict(UUID districtId);

  /** True when at least one ACTIVE warning of the event targets the district. */
  boolean hasActiveWarning(UUID eventId, UUID districtId);
}
