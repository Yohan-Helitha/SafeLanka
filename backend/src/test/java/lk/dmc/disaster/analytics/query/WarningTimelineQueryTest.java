package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.entity.AlertTimeline.TimelineEntry;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Kalu seed: three warnings, WATCH then WARNING then EVACUATE, each superseding the one before. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class WarningTimelineQueryTest {

  @Autowired WarningTimelineQuery query;

  private static ReportContext kalu(Set<UUID> districts, Instant from, Instant to) {
    return new ReportContext(TestIds.event(2), districts, from, to, TestIds.user(1));
  }

  @Test
  void wholeEvent_isThreeWarningsInTimeOrderWithASupersedesChain() {
    List<TimelineEntry> entries = query.execute(kalu(null, null, null));

    assertThat(entries).extracting(TimelineEntry::level).containsExactly("WATCH", "WARNING", "EVACUATE");
    assertThat(entries).extracting(TimelineEntry::issuedAt).isSorted();
    assertThat(entries.get(0).supersedesId()).isNull();
    assertThat(entries.get(1).supersedesId()).isEqualTo(entries.get(0).warningId());
    assertThat(entries.get(2).supersedesId()).isEqualTo(entries.get(1).warningId());
    assertThat(entries).allSatisfy(e -> assertThat(e.resolvedDistrictIds()).isNotEmpty());
  }

  @Test
  void aChosenDistrict_keepsTheWarningsThatReachIt() {
    var entries = query.execute(kalu(Set.of(TestIds.district(4)), null, null));

    assertThat(entries).isNotEmpty();
    assertThat(entries).allSatisfy(e -> assertThat(e.resolvedDistrictIds()).contains(TestIds.district(4)));
  }

  @Test
  void aDistrictNoWarningReaches_hasNoEntries() {
    assertThat(query.execute(kalu(Set.of(TestIds.district(1)), null, null))).isEmpty();
  }

  @Test
  void aWindowStartingAtTheSecondWarning_dropsTheFirst() {
    var all = query.execute(kalu(null, null, null));
    Instant second = all.get(1).issuedAt();

    assertThat(query.execute(kalu(null, second, null)))
        .extracting(TimelineEntry::level)
        .containsExactly("WARNING", "EVACUATE");
    assertThat(query.execute(kalu(null, null, second)))
        .extracting(TimelineEntry::level)
        .containsExactly("WATCH", "WARNING");
  }

  @Test
  void aWindowAfterTheEvent_hasNoEntries() {
    assertThat(query.execute(kalu(null, Instant.parse("2027-01-01T00:00:00Z"), null))).isEmpty();
  }

  @Test
  void aBasinWarning_reachesTheDistrictsOfThatBasin() {
    var kelani = new ReportContext(TestIds.event(1), null, null, null, TestIds.user(1));

    assertThat(query.execute(kelani))
        .allSatisfy(e -> assertThat(e.resolvedDistrictIds()).isNotEmpty());
  }
}
