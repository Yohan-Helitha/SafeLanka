package lk.dmc.disaster.analytics.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SectionKeyTest {

  @Test
  void sectionsAreDeclaredInReportOrder() {
    assertThat(SectionKey.values())
        .containsExactly(
            SectionKey.ALERT_TIMELINE,
            SectionKey.CITIZENS_REACHED,
            SectionKey.SHELTER_OCCUPANCY,
            SectionKey.RESOURCE_DISTRIBUTION);
  }

  @Test
  void apiNamesAreTheCamelCaseKeysOfAReportResponse() {
    assertThat(SectionKey.ALERT_TIMELINE.apiName()).isEqualTo("alertTimeline");
    assertThat(SectionKey.CITIZENS_REACHED.apiName()).isEqualTo("citizensReached");
    assertThat(SectionKey.SHELTER_OCCUPANCY.apiName()).isEqualTo("shelterOccupancy");
    assertThat(SectionKey.RESOURCE_DISTRIBUTION.apiName()).isEqualTo("resourceDistribution");
  }

  @ParameterizedTest
  @EnumSource(SectionKey.class)
  void fromAnyName_findsASectionByEitherSpelling(SectionKey key) {
    assertThat(SectionKey.fromAnyName(key.name())).contains(key);
    assertThat(SectionKey.fromAnyName(key.apiName())).contains(key);
  }

  @Test
  void fromAnyName_isEmptyForAnUnknownNameOrNull() {
    assertThat(SectionKey.fromAnyName("SOMETHING_NEW")).isEmpty();
    assertThat(SectionKey.fromAnyName(null)).isEmpty();
  }
}
