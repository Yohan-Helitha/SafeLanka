package lk.dmc.disaster.warnings.service;

import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.warnings.entity.Warning;
import lk.dmc.disaster.warnings.entity.WarningTarget;

/**
 * A warning with everything a response needs, read while the database session was open. Outside the
 * service only the warning's plain fields may be used; its areas and evidence are copied here.
 */
public record WarningView(
    Warning warning,
    WarningTarget target,
    Set<UUID> evidenceReportIds,
    Set<UUID> resolvedDistrictIds,
    DeliveryOutcome deliveries) {

  public WarningView {
    evidenceReportIds = Set.copyOf(evidenceReportIds);
    resolvedDistrictIds = Set.copyOf(resolvedDistrictIds);
  }
}
