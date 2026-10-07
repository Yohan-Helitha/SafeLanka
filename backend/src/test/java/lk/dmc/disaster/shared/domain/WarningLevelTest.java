package lk.dmc.disaster.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WarningLevelTest {

  @Test
  void rank_followsSeverityOrder() {
    assertThat(WarningLevel.values()).extracting(WarningLevel::rank).containsExactly(1, 2, 3, 4);
  }

  @Test
  void isHigherThan_higherLevel_true() {
    assertThat(WarningLevel.EVACUATE.isHigherThan(WarningLevel.WARNING)).isTrue();
    assertThat(WarningLevel.WATCH.isHigherThan(WarningLevel.ADVISORY)).isTrue();
  }

  @Test
  void isHigherThan_sameOrLowerLevel_false() {
    assertThat(WarningLevel.WATCH.isHigherThan(WarningLevel.WATCH)).isFalse();
    assertThat(WarningLevel.ADVISORY.isHigherThan(WarningLevel.EVACUATE)).isFalse();
  }
}
