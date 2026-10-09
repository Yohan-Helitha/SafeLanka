package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.entity.EventSummary;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class EventSummaryQueryTest {

  @Autowired EventSummaryQuery query;

  @Test
  void noStatus_listsEveryEventNewestFirst() {
    List<EventSummary> all = query.execute(null);

    assertThat(all).extracting(EventSummary::id).contains(TestIds.event(1), TestIds.event(2));
    assertThat(all).extracting(EventSummary::startedAt).isSortedAccordingTo((a, b) -> b.compareTo(a));
  }

  @Test
  void kaluSummary_countsDistrictsWarningsAndVerifiedReports() {
    EventSummary kalu =
        query.execute(null).stream().filter(e -> e.id().equals(TestIds.event(2))).findFirst().orElseThrow();

    assertThat(kalu.status()).isEqualTo("CLOSED");
    assertThat(kalu.endedAt()).isNotNull();
    assertThat(kalu.districtIds()).contains(TestIds.district(3), TestIds.district(4));
    assertThat(kalu.warningCount()).isEqualTo(3);
    assertThat(kalu.reportCount()).isPositive();
  }

  @Test
  void statusFilter_keepsOnlyThatStatus() {
    assertThat(query.execute("ACTIVE")).isNotEmpty().allMatch(e -> e.status().equals("ACTIVE"));
    assertThat(query.execute("CLOSED")).isNotEmpty().allMatch(e -> e.status().equals("CLOSED"));
    assertThat(query.execute("ACTIVE")).extracting(EventSummary::id).contains(TestIds.event(1));
    assertThat(query.execute("ACTIVE")).extracting(EventSummary::endedAt).containsOnlyNulls();
  }

  @Test
  void unknownStatus_hasNothing() {
    assertThat(query.execute("NOT_A_STATUS")).isEmpty();
  }
}
