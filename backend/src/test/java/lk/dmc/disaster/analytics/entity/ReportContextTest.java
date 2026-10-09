package lk.dmc.disaster.analytics.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;

class ReportContextTest {

  private static ReportContext with(Set<java.util.UUID> districts) {
    return new ReportContext(TestIds.event(2), districts, null, null, TestIds.user(1));
  }

  @Test
  void noDistricts_isGlobal() {
    assertThat(with(null).isGlobal()).isTrue();
    assertThat(with(Set.of()).isGlobal()).isTrue();
  }

  @Test
  void chosenDistricts_isNotGlobal() {
    assertThat(with(Set.of(TestIds.district(4))).isGlobal()).isFalse();
  }
}
