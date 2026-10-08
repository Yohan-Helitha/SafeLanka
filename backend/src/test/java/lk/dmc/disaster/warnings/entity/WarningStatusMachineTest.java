package lk.dmc.disaster.warnings.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WarningStatusMachineTest {

  @ParameterizedTest
  @EnumSource(
      value = WarningStatus.class,
      names = {"CANCELLED", "EXPIRED"})
  void canTransition_fromActiveToAnyEnd_isAllowed(WarningStatus to) {
    assertThat(WarningStatusMachine.canTransition(WarningStatus.ACTIVE, to)).isTrue();
  }

  @Test
  void canTransition_activeToEscalated_isForbiddenBecauseEscalationRaisesTheSameWarning() {
    assertThat(WarningStatusMachine.canTransition(WarningStatus.ACTIVE, WarningStatus.ESCALATED))
        .isFalse();
  }

  @Test
  void canTransition_activeToActive_isForbidden() {
    assertThat(WarningStatusMachine.canTransition(WarningStatus.ACTIVE, WarningStatus.ACTIVE))
        .isFalse();
  }

  @ParameterizedTest
  @EnumSource(
      value = WarningStatus.class,
      names = {"ESCALATED", "CANCELLED", "EXPIRED"})
  void canTransition_fromFinalStatus_isForbiddenToEverything(WarningStatus from) {
    for (WarningStatus to : WarningStatus.values()) {
      assertThat(WarningStatusMachine.canTransition(from, to)).isFalse();
    }
  }

  @Test
  void require_forbiddenChange_throwsInvalidStateTransition() {
    assertThatThrownBy(
            () -> WarningStatusMachine.require(WarningStatus.CANCELLED, WarningStatus.ACTIVE))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION))
        .hasMessageContaining("CANCELLED")
        .hasMessageContaining("ACTIVE");
  }

  @Test
  void require_allowedChange_doesNotThrow() {
    WarningStatusMachine.require(WarningStatus.ACTIVE, WarningStatus.CANCELLED);
  }
}
