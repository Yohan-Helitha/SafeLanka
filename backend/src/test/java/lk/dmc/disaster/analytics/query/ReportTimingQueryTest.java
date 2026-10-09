package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ReportTimingQueryTest {

  @Autowired ReportTimingQuery query;

  private static ReportContext of(UUID eventId) {
    return new ReportContext(eventId, null, null, null, TestIds.user(1));
  }

  @Test
  void kaluEvent_hasTheFirstVerifiedReviewTime() {
    assertThat(query.execute(of(TestIds.event(2)))).contains(Instant.parse("2026-05-14T00:05:00Z"));
  }

  @Test
  void anEventWithoutVerifiedReports_hasNone() {
    assertThat(query.execute(of(UUID.randomUUID()))).isEmpty();
  }
}
