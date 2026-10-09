package lk.dmc.disaster.analytics.service;

import static lk.dmc.disaster.analytics.AnalyticsFixtures.AUTHOR_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.ReportContext;
import lk.dmc.disaster.analytics.entity.SectionKey;
import lk.dmc.disaster.analytics.entity.SectionResult;
import lk.dmc.disaster.analytics.section.ReportSection;
import org.junit.jupiter.api.Test;

class ReportBuilderTest {

  private static final Instant NOW = Instant.parse("2026-10-04T08:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final ReportContext CONTEXT =
      new ReportContext(EVENT_ID, null, null, null, AUTHOR_ID);

  /** A section that returns what it is given. */
  private static ReportSection section(SectionKey key, SectionResult<?> result) {
    return new ReportSection() {
      @Override
      public SectionKey getKey() {
        return key;
      }

      @Override
      public SectionResult<?> generate(ReportContext context) {
        return result;
      }
    };
  }

  private static ReportSection failing(SectionKey key) {
    return new ReportSection() {
      @Override
      public SectionKey getKey() {
        return key;
      }

      @Override
      public SectionResult<?> generate(ReportContext context) {
        throw new IllegalStateException("query failed");
      }
    };
  }

  @Test
  void build_collectsAvailableDataAndUnavailableReasons() {
    var builder =
        new ReportBuilder(
            List.of(
                section(SectionKey.ALERT_TIMELINE, SectionResult.success(SectionKey.ALERT_TIMELINE, "DATA")),
                section(
                    SectionKey.CITIZENS_REACHED,
                    SectionResult.unavailable(SectionKey.CITIZENS_REACHED, "No deliveries"))),
            CLOCK);

    var report = builder.build(CONTEXT);

    assertThat(report.getSections()).containsOnlyKeys("ALERT_TIMELINE").containsEntry("ALERT_TIMELINE", "DATA");
    assertThat(report.getUnavailableSections())
        .containsExactly(java.util.Map.of("key", "CITIZENS_REACHED", "reason", "No deliveries"));
  }

  @Test
  void build_runsSectionsInReportOrderWhateverOrderTheyAreGiven() {
    var builder =
        new ReportBuilder(
            List.of(
                section(SectionKey.RESOURCE_DISTRIBUTION, SectionResult.success(SectionKey.RESOURCE_DISTRIBUTION, "D")),
                section(SectionKey.SHELTER_OCCUPANCY, SectionResult.success(SectionKey.SHELTER_OCCUPANCY, "C")),
                section(SectionKey.ALERT_TIMELINE, SectionResult.success(SectionKey.ALERT_TIMELINE, "A")),
                section(SectionKey.CITIZENS_REACHED, SectionResult.success(SectionKey.CITIZENS_REACHED, "B"))),
            CLOCK);

    var report = builder.build(CONTEXT);

    assertThat(report.getSections().keySet())
        .containsExactly("ALERT_TIMELINE", "CITIZENS_REACHED", "SHELTER_OCCUPANCY", "RESOURCE_DISTRIBUTION");
  }

  @Test
  void build_aSectionThatThrowsIsListedAsUnavailableAndTheOthersStillRun() {
    var builder =
        new ReportBuilder(
            List.of(
                failing(SectionKey.ALERT_TIMELINE),
                section(SectionKey.CITIZENS_REACHED, SectionResult.success(SectionKey.CITIZENS_REACHED, "B"))),
            CLOCK);

    var report = builder.build(CONTEXT);

    assertThat(report.getSections()).containsOnlyKeys("CITIZENS_REACHED");
    assertThat(report.getUnavailableSections())
        .containsExactly(
            java.util.Map.of("key", "ALERT_TIMELINE", "reason", ReportBuilder.SECTION_FAILED));
  }

  @Test
  void build_everySectionUnavailableStillProducesAReport() {
    var builder =
        new ReportBuilder(
            List.of(section(SectionKey.ALERT_TIMELINE, SectionResult.unavailable(SectionKey.ALERT_TIMELINE, "none"))),
            CLOCK);

    var report = builder.build(CONTEXT);

    assertThat(report.getSections()).isEmpty();
    assertThat(report.getUnavailableSections()).hasSize(1);
  }

  @Test
  void build_withNoSectionsAtAll_isAnEmptyReport() {
    var report = new ReportBuilder(List.of(), CLOCK).build(CONTEXT);

    assertThat(report.getSections()).isEmpty();
    assertThat(report.getUnavailableSections()).isEmpty();
  }

  @Test
  void build_stampsTheReportWithTheEventTheAuthorAndTheClock() {
    var report = new ReportBuilder(List.of(), CLOCK).build(CONTEXT);

    assertThat(report.getEventId()).isEqualTo(EVENT_ID);
    assertThat(report.getGeneratedBy()).isEqualTo(AUTHOR_ID);
    assertThat(report.getGeneratedAt()).isEqualTo(NOW);
  }

  @Test
  void build_savesTheFiltersAsDistrictIdsFromAndTo() {
    UUID b = UUID.fromString("00000000-0000-0000-0001-000000000002");
    UUID a = UUID.fromString("00000000-0000-0000-0001-000000000001");
    var context =
        new ReportContext(
            EVENT_ID,
            Set.of(b, a),
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-10T00:00:00Z"),
            AUTHOR_ID);

    var report = new ReportBuilder(List.of(), CLOCK).build(context);

    assertThat(report.getFilters())
        .containsEntry("districtIds", List.of(a.toString(), b.toString()))
        .containsEntry("from", "2026-05-01T00:00:00Z")
        .containsEntry("to", "2026-05-10T00:00:00Z");
  }

  @Test
  void build_leavesOutFiltersThatWereNotSet() {
    var report = new ReportBuilder(List.of(), CLOCK).build(CONTEXT);

    assertThat(report.getFilters()).isEmpty();
  }
}
