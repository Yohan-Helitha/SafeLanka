package lk.dmc.disaster.warnings.entity;

import static lk.dmc.disaster.warnings.entity.HazardStatus.MONITORING;
import static lk.dmc.disaster.warnings.entity.HazardStatus.RESOLVED;
import static lk.dmc.disaster.warnings.entity.HazardStatus.UNDER_ASSESSMENT;
import static lk.dmc.disaster.warnings.entity.HazardStatus.WARNED;

/** Allowed hazard status changes. RESOLVED is final. */
public final class HazardStatusMachine {

  private static final StatusMachine<HazardStatus> MACHINE =
      StatusMachine.builder("Hazard", HazardStatus.class)
          .allow(UNDER_ASSESSMENT, WARNED, MONITORING, RESOLVED)
          .allow(MONITORING, WARNED, RESOLVED)
          .allow(WARNED, MONITORING, RESOLVED)
          .build();

  private HazardStatusMachine() {}

  public static boolean canTransition(HazardStatus from, HazardStatus to) {
    return MACHINE.canTransition(from, to);
  }

  /**
   * Checks a status change.
   *
   * @throws lk.dmc.disaster.shared.error.AppException INVALID_STATE_TRANSITION when not allowed
   */
  public static void require(HazardStatus from, HazardStatus to) {
    MACHINE.require(from, to);
  }
}
