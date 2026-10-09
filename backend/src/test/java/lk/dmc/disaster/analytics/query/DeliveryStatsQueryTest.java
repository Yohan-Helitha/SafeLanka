package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.entity.CitizensReached.ChannelStats;
import lk.dmc.disaster.analytics.entity.CitizensReached.DistrictStats;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Against the Kalu Flood May 2026 seed: 17 citizens in Ratnapura (8) and Kalutara (9). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DeliveryStatsQueryTest {

  private static final UUID KALU = TestIds.event(2);
  private static final UUID KELANI = TestIds.event(1);

  @Autowired DeliveryStatsQuery query;

  private static ReportContext kalu(Set<UUID> districts, Instant from, Instant to) {
    return new ReportContext(KALU, districts, from, to, TestIds.user(1));
  }

  @Test
  void wholeEvent_targetedAndReached_are17() {
    var ctx = kalu(null, null, null);

    assertThat(query.getUniqueTargeted(ctx)).isEqualTo(17);
    assertThat(query.getUniqueReached(ctx)).isEqualTo(17);
  }

  @Test
  void byDistrict_splitsTheCitizensBetweenRatnapuraAndKalutara() {
    var stats = query.getDistrictStats(kalu(null, null, null));

    assertThat(stats).extracting(DistrictStats::districtName).containsExactly("Kalutara", "Ratnapura");
    assertThat(stats).filteredOn(d -> d.districtName().equals("Ratnapura"))
        .singleElement().satisfies(d -> {
          assertThat(d.targeted()).isEqualTo(8);
          assertThat(d.reached()).isEqualTo(8);
        });
    assertThat(stats).filteredOn(d -> d.districtName().equals("Kalutara"))
        .singleElement().extracting(DistrictStats::targeted).isEqualTo(9L);
  }

  @Test
  void byChannel_countsDeliveredAndFailed_andSomeSmsFailed() {
    var channels = query.getChannelStats(kalu(null, null, null));

    assertThat(channels).extracting(ChannelStats::channel).contains("PUSH", "SMS", "AUDIBLE");
    ChannelStats sms = channels.stream().filter(c -> c.channel().equals("SMS")).findFirst().orElseThrow();
    assertThat(sms.failed()).isPositive();
    assertThat(sms.delivered()).isGreaterThan(sms.failed());
  }

  @Test
  void narrowedToOneDistrict_countsOnlyItsCitizens() {
    var ratnapura = kalu(Set.of(TestIds.district(4)), null, null);

    assertThat(query.getUniqueTargeted(ratnapura)).isEqualTo(8);
    assertThat(query.getUniqueReached(ratnapura)).isEqualTo(8);
    assertThat(query.getDistrictStats(ratnapura)).extracting(DistrictStats::districtName).containsExactly("Ratnapura");
    assertThat(query.getChannelStats(ratnapura)).isNotEmpty();
  }

  @Test
  void aDistrictOutsideTheEvent_hasNoDeliveries() {
    var colombo = kalu(Set.of(TestIds.district(1)), null, null);

    assertThat(query.getUniqueTargeted(colombo)).isZero();
    assertThat(query.getUniqueReached(colombo)).isZero();
    assertThat(query.getChannelStats(colombo)).isEmpty();
    assertThat(query.getDistrictStats(colombo)).isEmpty();
  }

  @Test
  void aWindowAroundTheEvent_keepsEverything() {
    var around = kalu(null, Instant.parse("2026-05-14T00:00:00Z"), Instant.parse("2026-05-17T00:00:00Z"));

    assertThat(query.getUniqueTargeted(around)).isEqualTo(17);
  }

  @Test
  void aWindowAfterTheEvent_hasNothing() {
    var later = kalu(null, Instant.parse("2027-01-01T00:00:00Z"), null);

    assertThat(query.getUniqueTargeted(later)).isZero();
    assertThat(query.getDistrictStats(later)).isEmpty();
  }

  @Test
  void aWindowEndingBeforeTheFirstWarning_hasNothing() {
    var earlier = kalu(null, null, Instant.parse("2026-05-14T00:00:00Z"));

    assertThat(query.getUniqueTargeted(earlier)).isZero();
  }

  @Test
  void anotherEventsDeliveriesAreNotCounted() {
    var kelani = new ReportContext(KELANI, null, null, null, TestIds.user(1));

    assertThat(query.getDistrictStats(kelani))
        .extracting(DistrictStats::districtName)
        .doesNotContain("Ratnapura", "Kalutara");
  }
}
