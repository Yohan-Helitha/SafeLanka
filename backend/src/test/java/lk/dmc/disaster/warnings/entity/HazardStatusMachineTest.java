package lk.dmc.disaster.warnings.entity;

import static lk.dmc.disaster.warnings.entity.HazardStatus.MONITORING;
import static lk.dmc.disaster.warnings.entity.HazardStatus.RESOLVED;
import static lk.dmc.disaster.warnings.entity.HazardStatus.UNDER_ASSESSMENT;
import static lk.dmc.disaster.warnings.entity.HazardStatus.WARNED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HazardStatusMachineTest {

  private static final Map<HazardStatus, Set<HazardStatus>> ALLOWED =
      Map.of(
          UNDER_ASSESSMENT, Set.of(WARNED, MONITORING, RESOLVED),
          MONITORING, Set.of(WARNED, RESOLVED),
          WARNED, Set.of(MONITORING, RESOLVED),
          RESOLVED, Set.of());

  @ParameterizedTest
  @EnumSource(HazardStatus.class)
  void canTransition_everyPair_matchesTheSpecification(HazardStatus from) {
    for (HazardStatus to : HazardStatus.values()) {
      assertThat(HazardStatusMachine.canTransition(from, to))
          .as("%s -> %s", from, to)
          .isEqualTo(ALLOWED.get(from).contains(to));
    }
  }

  @Test
  void canTransition_resolvedIsFinal() {
    for (HazardStatus to : HazardStatus.values()) {
      assertThat(HazardStatusMachine.canTransition(RESOLVED, to)).isFalse();
    }
  }

  @Test
  void require_forbiddenChange_throwsInvalidStateTransition() {
    assertThatThrownBy(() -> HazardStatusMachine.require(RESOLVED, MONITORING))
        .isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void require_allowedChange_doesNotThrow() {
    HazardStatusMachine.require(MONITORING, WARNED);
  }
}
