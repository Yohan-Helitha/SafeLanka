package lk.dmc.disaster.warnings.entity;

import static lk.dmc.disaster.warnings.entity.WarningStatus.ACTIVE;
import static lk.dmc.disaster.warnings.entity.WarningStatus.CANCELLED;
import static lk.dmc.disaster.warnings.entity.WarningStatus.EXPIRED;

/** Allowed warning status changes: ACTIVE may end two ways, every other status is final. */
public final class WarningStatusMachine {

  private static final StatusMachine<WarningStatus> MACHINE =
      StatusMachine.builder("Warning", WarningStatus.class)
          .allow(ACTIVE, CANCELLED, EXPIRED)
          .build();

  private WarningStatusMachine() {}

  public static boolean canTransition(WarningStatus from, WarningStatus to) {
    return MACHINE.canTransition(from, to);
  }

  /**
   * Checks a status change.
   *
   * @throws lk.dmc.disaster.shared.error.AppException INVALID_STATE_TRANSITION when not allowed
   */
  public static void require(WarningStatus from, WarningStatus to) {
    MACHINE.require(from, to);
  }
}
