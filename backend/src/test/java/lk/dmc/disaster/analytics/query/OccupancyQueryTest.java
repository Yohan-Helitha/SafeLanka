package lk.dmc.disaster.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.TestcontainersConfiguration;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.query.OccupancyQuery.ShelterSeriesRecord;
import lk.dmc.disaster.support.TestIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Against the Kalu seed: one shelter, twelve readings (20, 85, 160, 240, 285, 290, 260, 180, 110,
 * 60, 20, 0). Tests that insert rows are transactional, so they roll back.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OccupancyQueryTest {

  private static final UUID KALU = TestIds.event(2);

  @Autowired OccupancyQuery query;
  @Autowired JdbcClient jdbc;

  private static ReportContext kalu(Set<UUID> districts, Instant from, Instant to) {
    return new ReportContext(KALU, districts, from, to, TestIds.user(1));
  }

  @Test
  void series_isTwelveReadingsInTimeOrder() {
    var series = query.getSeries(kalu(null, null, null));

    assertThat(series).extracting(ShelterSeriesRecord::occupancy)
        .containsExactly(20, 85, 160, 240, 285, 290, 260, 180, 110, 60, 20, 0);
    assertThat(series).extracting(ShelterSeriesRecord::recordedAt).isSorted();
    assertThat(series).extracting(ShelterSeriesRecord::capacity).containsOnly(300);
  }

  @Test
  void peaks_isOneRowPerShelterAt290() {
    var peaks = query.getPeaks(kalu(null, null, null));

    assertThat(peaks).singleElement().satisfies(p -> {
      assertThat(p.peakOccupancy()).isEqualTo(290);
      assertThat(p.capacity()).isEqualTo(300);
    });
  }

  @Test
  void aDistrictWithoutTheShelter_hasNoReadings() {
    var kalutara = kalu(Set.of(TestIds.district(3)), null, null);

    assertThat(query.getSeries(kalutara)).isEmpty();
    assertThat(query.getPeaks(kalutara)).isEmpty();
  }

  @Test
  void theShelterDistrict_hasAllReadings() {
    assertThat(query.getSeries(kalu(Set.of(TestIds.district(4)), null, null))).hasSize(12);
  }

  @Test
  void windowBoundariesAreInclusive() {
    List<ShelterSeriesRecord> all = query.getSeries(kalu(null, null, null));
    Instant first = all.get(0).recordedAt();
    Instant second = all.get(1).recordedAt();

    assertThat(query.getSeries(kalu(null, first, first))).hasSize(1);
    assertThat(query.getSeries(kalu(null, first, second))).hasSize(2);
    assertThat(query.getSeries(kalu(null, second, null))).hasSize(11);
    assertThat(query.getSeries(kalu(null, null, first))).hasSize(1);
  }

  @Test
  void aWindowAfterThePeak_hasALowerPeak() {
    List<ShelterSeriesRecord> all = query.getSeries(kalu(null, null, null));
    Instant afterPeak = all.get(6).recordedAt();

    assertThat(query.getPeaks(kalu(null, afterPeak, null)))
        .singleElement()
        .satisfies(p -> assertThat(p.peakOccupancy()).isEqualTo(260));
  }

  @Test
  void aWindowOutsideTheEvent_hasNothing() {
    var later = kalu(null, Instant.parse("2027-01-01T00:00:00Z"), null);

    assertThat(query.getSeries(later)).isEmpty();
    assertThat(query.getPeaks(later)).isEmpty();
  }

  @Test
  void anotherEventsReadingsAreExcluded() {
    var kelani = new ReportContext(TestIds.event(1), Set.of(TestIds.district(4)), null, null, TestIds.user(1));

    assertThat(query.getSeries(kelani)).isEmpty();
  }

  @Test
  @Transactional
  void whenTheHighestReadingIsReachedTwice_thePeakIsTheEarlierOne() {
    ShelterSeriesRecord any = query.getSeries(kalu(null, null, null)).get(0);
    Instant first = Instant.parse("2026-05-20T10:00:00Z");
    Instant second = Instant.parse("2026-05-20T22:00:00Z");
    for (Instant at : List.of(second, first)) {
      jdbc.sql(
              "INSERT INTO occupancy_logs (shelter_id, event_id, occupancy, delta, recorded_by, recorded_at) "
                  + "VALUES (:shelter, :event, 295, 5, :user, :at)")
          .param("shelter", any.shelterId())
          .param("event", KALU)
          .param("user", TestIds.user(1))
          .param("at", java.time.OffsetDateTime.ofInstant(at, java.time.ZoneOffset.UTC))
          .update();
    }

    var peak = query.getPeaks(kalu(null, null, null));

    assertThat(peak).singleElement().satisfies(p -> {
      assertThat(p.peakOccupancy()).isEqualTo(295);
      assertThat(p.peakAt()).isEqualTo(first);
    });
  }
}
