package lk.dmc.disaster.analytics.mapper;

import static lk.dmc.disaster.analytics.AnalyticsFixtures.AUTHOR_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.GENERATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.analytics.AnalyticsFixtures;
import lk.dmc.disaster.analytics.dto.GenerateReportRequest;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.shared.reference.ReferenceData;
import lk.dmc.disaster.shared.reference.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsMapperTest {

  @Mock ReferenceData referenceData;
  @Mock UserDirectory userDirectory;

  private AnalyticsMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AnalyticsMapper(referenceData, userDirectory);
  }

  private void givenNames() {
    when(referenceData.event(EVENT_ID)).thenReturn(AnalyticsFixtures.closedEvent());
    when(userDirectory.require(AUTHOR_ID)).thenReturn(AnalyticsFixtures.author());
  }

  private static DisasterReport saved(
      Map<String, Object> filters,
      Map<String, Object> sections,
      List<Map<String, String>> unavailable) {
    return new DisasterReport(EVENT_ID, filters, sections, unavailable, AUTHOR_ID, GENERATED_AT);
  }

  @Test
  void toContext_carriesTheRequestAndTheAuthor() {
    UUID district = UUID.randomUUID();
    Instant from = Instant.parse("2026-05-14T01:00:00Z");
    Instant to = Instant.parse("2026-05-15T01:00:00Z");

    var context = mapper.toContext(new GenerateReportRequest(EVENT_ID, Set.of(district), from, to), AUTHOR_ID);

    assertThat(context.eventId()).isEqualTo(EVENT_ID);
    assertThat(context.districtIds()).containsExactly(district);
    assertThat(context.fromTime()).isEqualTo(from);
    assertThat(context.toTime()).isEqualTo(to);
    assertThat(context.generatedBy()).isEqualTo(AUTHOR_ID);
  }

  @Test
  void toResponse_namesTheEventAndTheAuthor() {
    givenNames();

    var response = mapper.toResponse(saved(Map.of(), Map.of(), List.of()));

    assertThat(response.eventName()).isEqualTo("Kalu Flood May 2026");
    assertThat(response.generatedBy().id()).isEqualTo(AUTHOR_ID);
    assertThat(response.generatedBy().fullName()).isEqualTo("Nimal Perera");
    assertThat(response.generatedAt()).isEqualTo(GENERATED_AT);
  }

  @Test
  void toResponse_sectionsComeBackUnderCamelCaseKeysInReportOrder() {
    givenNames();
    Map<String, Object> stored = new LinkedHashMap<>();
    stored.put("RESOURCE_DISTRIBUTION", Map.of("x", 1));
    stored.put("ALERT_TIMELINE", Map.of("y", 2));

    var response = mapper.toResponse(saved(Map.of(), stored, List.of()));

    assertThat(response.sections().keySet()).containsExactly("alertTimeline", "resourceDistribution");
  }

  @Test
  void toResponse_acceptsReportsSavedWithCamelCaseKeysAsWell() {
    givenNames();

    var response = mapper.toResponse(saved(Map.of(), Map.of("citizensReached", Map.of("z", 3)), List.of()));

    assertThat(response.sections()).containsOnlyKeys("citizensReached");
  }

  @Test
  void toResponse_filtersAreDistrictIdsFromAndTo() {
    givenNames();
    var filters = Map.<String, Object>of("districtIds", List.of("d1"), "from", "2026-05-14T00:10:00Z", "to", "2026-05-15T00:00:00Z");

    var response = mapper.toResponse(saved(filters, Map.of(), List.of()));

    assertThat(response.filters())
        .containsEntry("districtIds", List.of("d1"))
        .containsEntry("from", "2026-05-14T00:10:00Z")
        .containsEntry("to", "2026-05-15T00:00:00Z");
  }

  @Test
  void toResponse_readsTheOlderFromTimeAndToTimeFilterNames() {
    givenNames();
    var legacy = Map.<String, Object>of("fromTime", "2026-05-14T00:10:00Z", "toTime", "2026-05-15T00:00:00Z");

    var response = mapper.toResponse(saved(legacy, Map.of(), List.of()));

    assertThat(response.filters())
        .containsEntry("from", "2026-05-14T00:10:00Z")
        .containsEntry("to", "2026-05-15T00:00:00Z");
  }

  @Test
  void toResponse_aReportWithoutFiltersHasEmptyDistrictsAndNullDates() {
    givenNames();

    var response = mapper.toResponse(saved(Map.of(), Map.of(), List.of()));

    assertThat(response.filters()).containsEntry("districtIds", List.of());
    assertThat(response.filters().get("from")).isNull();
    assertThat(response.filters().get("to")).isNull();
  }

  @Test
  void toResponse_keepsTheUnavailableSectionsAndTheirReasons() {
    givenNames();
    var unavailable = List.of(Map.of("key", "SHELTER_OCCUPANCY", "reason", "No shelter occupancy was logged for this event."));

    var response = mapper.toResponse(saved(Map.of(), Map.of(), unavailable));

    assertThat(response.unavailableSections()).isEqualTo(unavailable);
  }

  @Test
  void toSummaryResponse_countsTheUnavailableSections() {
    givenNames();
    var unavailable = List.of(Map.of("key", "A", "reason", "r"), Map.of("key", "B", "reason", "r"));

    var summary = mapper.toSummaryResponse(saved(Map.of(), Map.of(), unavailable));

    assertThat(summary.eventName()).isEqualTo("Kalu Flood May 2026");
    assertThat(summary.generatedByName()).isEqualTo("Nimal Perera");
    assertThat(summary.unavailableCount()).isEqualTo(2);
  }

  @Test
  void toSummaryResponse_aReportWithNoUnavailableListCountsZero() {
    givenNames();

    assertThat(mapper.toSummaryResponse(saved(Map.of(), Map.of(), null)).unavailableCount()).isZero();
  }
}
