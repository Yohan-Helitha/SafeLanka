package lk.dmc.disaster.reports.entity;

import static lk.dmc.disaster.reports.entity.ReportStatus.NEEDS_MORE_INFO;
import static lk.dmc.disaster.reports.entity.ReportStatus.PENDING;
import static lk.dmc.disaster.reports.entity.ReportStatus.REJECTED;
import static lk.dmc.disaster.reports.entity.ReportStatus.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lk.dmc.disaster.shared.error.ErrorCode;
import lk.dmc.disaster.shared.error.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ReportStatusMachineTest {

  private static final Set<List<ReportStatus>> ALLOWED =
      Set.of(
          List.of(PENDING, VERIFIED),
          List.of(PENDING, REJECTED),
          List.of(PENDING, NEEDS_MORE_INFO),
          List.of(NEEDS_MORE_INFO, VERIFIED),
          List.of(NEEDS_MORE_INFO, REJECTED));

  static Stream<Arguments> allowed() {
    return ALLOWED.stream().map(pair -> Arguments.of(pair.get(0), pair.get(1)));
  }

  static Stream<Arguments> forbidden() {
    return Stream.of(ReportStatus.values())
        .flatMap(from -> Stream.of(ReportStatus.values()).map(to -> List.of(from, to)))
        .filter(pair -> !ALLOWED.contains(pair))
        .map(pair -> Arguments.of(pair.get(0), pair.get(1)));
  }

  @ParameterizedTest
  @MethodSource("allowed")
  void transition_allowedChangeReturnsTarget(ReportStatus from, ReportStatus to) {
    assertThat(ReportStatusMachine.canTransition(from, to)).isTrue();
    assertThat(ReportStatusMachine.transition(from, to)).isEqualTo(to);
  }

  @ParameterizedTest
  @MethodSource("forbidden")
  void transition_forbiddenChangeThrowsConflict(ReportStatus from, ReportStatus to) {
    assertThat(ReportStatusMachine.canTransition(from, to)).isFalse();
    assertThatThrownBy(() -> ReportStatusMachine.transition(from, to))
        .isInstanceOfSatisfying(
            InvalidStateTransitionException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void forbidden_coversEverySelfTransitionAndEveryExitFromFinalStatuses() {
    assertThat(forbidden().count()).isEqualTo(11);
  }

  @ParameterizedTest
  @EnumSource(ReportStatus.class)
  void isFinal_onlyVerifiedAndRejected(ReportStatus status) {
    assertThat(status.isFinal()).isEqualTo(status == VERIFIED || status == REJECTED);
  }
}
