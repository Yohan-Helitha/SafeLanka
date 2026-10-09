package lk.dmc.disaster.reports.entity;

import static lk.dmc.disaster.reports.entity.ReportStatus.NEEDS_MORE_INFO;
import static lk.dmc.disaster.reports.entity.ReportStatus.PENDING;
import static lk.dmc.disaster.reports.entity.ReportStatus.REJECTED;
import static lk.dmc.disaster.reports.entity.ReportStatus.VERIFIED;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import lk.dmc.disaster.shared.error.InvalidStateTransitionException;

/** The only place that knows which status changes are allowed (State pattern). */
public final class ReportStatusMachine {

  private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED =
      new EnumMap<>(ReportStatus.class);

  static {
    ALLOWED.put(PENDING, Set.of(VERIFIED, REJECTED, NEEDS_MORE_INFO));
    // PENDING again when the reporter answers the officer's question
    ALLOWED.put(NEEDS_MORE_INFO, Set.of(PENDING, VERIFIED, REJECTED));
    ALLOWED.put(VERIFIED, Set.of());
    ALLOWED.put(REJECTED, Set.of());
  }

  private ReportStatusMachine() {}

  public static boolean canTransition(ReportStatus from, ReportStatus to) {
    return ALLOWED.get(from).contains(to);
  }

  /**
   * @return {@code to}, when the change is allowed
   * @throws InvalidStateTransitionException (409) when it is not
   */
  public static ReportStatus transition(ReportStatus from, ReportStatus to) {
    if (!canTransition(from, to)) {
      throw new InvalidStateTransitionException(from, to);
    }
    return to;
  }
}
