package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.ContextFilters.Filter;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;

class ContextFiltersTest {

  private static final Instant FROM = Instant.parse("2026-05-14T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-05-15T00:00:00Z");

  private static ReportContext context(Set<UUID> districts, Instant from, Instant to) {
    return new ReportContext(TestIds.event(2), districts, from, to, TestIds.user(1));
  }

  @Test
  void window_withNeitherEnd_addsNothing() {
    Filter filter = ContextFilters.window(context(null, null, null), "o.recorded_at");

    assertThat(filter.sql()).isEmpty();
    assertThat(filter.params()).isEmpty();
  }

  @Test
  void window_withOnlyAStart_addsOnlyTheLowerBound() {
    Filter filter = ContextFilters.window(context(null, FROM, null), "o.recorded_at");

    assertThat(filter.sql()).isEqualTo("AND o.recorded_at >= :fromTime ");
    assertThat(filter.params()).containsOnlyKeys("fromTime");
  }

  @Test
  void window_withOnlyAnEnd_addsOnlyTheUpperBound() {
    Filter filter = ContextFilters.window(context(null, null, TO), "o.recorded_at");

    assertThat(filter.sql()).isEqualTo("AND o.recorded_at <= :toTime ");
    assertThat(filter.params()).containsOnlyKeys("toTime");
  }

  @Test
  void window_withBothEnds_isInclusiveAndBindsUtcTimes() {
    Filter filter = ContextFilters.window(context(null, FROM, TO), "w.issued_at");

    assertThat(filter.sql()).isEqualTo("AND w.issued_at >= :fromTime AND w.issued_at <= :toTime ");
    assertThat(filter.params())
        .containsEntry("fromTime", OffsetDateTime.of(2026, 5, 14, 0, 0, 0, 0, ZoneOffset.UTC))
        .containsEntry("toTime", OffsetDateTime.of(2026, 5, 15, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  @Test
  void districts_notNarrowed_addsNothing() {
    assertThat(ContextFilters.districts(context(null, null, null), "s.district_id").sql()).isEmpty();
    assertThat(ContextFilters.districts(context(Set.of(), null, null), "s.district_id").sql()).isEmpty();
  }

  @Test
  void districts_narrowed_matchesAnyOfTheChosenOnes() {
    Filter filter =
        ContextFilters.districts(context(Set.of(TestIds.district(4)), null, null), "s.district_id");

    assertThat(filter.sql()).isEqualTo("AND s.district_id = ANY(CAST(:districtIds AS uuid[])) ");
    assertThat((UUID[]) filter.params().get("districtIds")).containsExactly(TestIds.district(4));
  }

  @Test
  void of_combinesTheWindowAndTheDistrictsInOneFilter() {
    Filter filter =
        ContextFilters.of(context(Set.of(TestIds.district(4)), FROM, null), "o.recorded_at", "s.district_id");

    assertThat(filter.sql())
        .isEqualTo(
            "AND o.recorded_at >= :fromTime AND s.district_id = ANY(CAST(:districtIds AS uuid[])) ");
    assertThat(filter.params()).containsOnlyKeys("fromTime", "districtIds");
  }

  @Test
  void of_withNothingAskedFor_isEmpty() {
    Filter filter = ContextFilters.of(context(null, null, null), "o.recorded_at", "s.district_id");

    assertThat(filter.sql()).isEmpty();
    assertThat(filter.params()).isEmpty();
  }
}
