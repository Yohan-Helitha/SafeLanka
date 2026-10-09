package lk.dmc.disaster.analytics;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.analytics.entity.DisasterReport;
import lk.dmc.disaster.shared.domain.Role;
import lk.dmc.disaster.shared.reference.DisasterEventView;
import lk.dmc.disaster.shared.reference.EventStatus;
import lk.dmc.disaster.shared.reference.UserView;
import lk.dmc.disaster.support.TestIds;

/** Builds events, users and saved reports for analytics unit tests. */
public final class AnalyticsFixtures {

  public static final UUID EVENT_ID = TestIds.event(2);
  public static final UUID RATNAPURA = TestIds.district(4);
  public static final UUID KALUTARA = TestIds.district(3);
  public static final UUID AUTHOR_ID = TestIds.user(1);
  public static final Instant EVENT_START = Instant.parse("2026-05-14T00:10:00Z");
  public static final Instant EVENT_END = Instant.parse("2026-05-16T00:00:00Z");
  public static final Instant GENERATED_AT = Instant.parse("2026-10-04T08:00:00Z");

  private AnalyticsFixtures() {}

  /** A closed event in Ratnapura and Kalutara, 14 to 16 May 2026. */
  public static DisasterEventView closedEvent() {
    return new DisasterEventView(
        EVENT_ID,
        "Kalu Flood May 2026",
        TestIds.hazardType(1),
        EventStatus.CLOSED,
        EVENT_START.atOffset(ZoneOffset.UTC),
        EVENT_END.atOffset(ZoneOffset.UTC),
        Set.of(RATNAPURA, KALUTARA));
  }

  /** An event that has started and not ended. */
  public static DisasterEventView activeEvent(Instant startedAt) {
    return new DisasterEventView(
        TestIds.event(1),
        "Kelani Flood October 2026",
        TestIds.hazardType(1),
        EventStatus.ACTIVE,
        OffsetDateTime.ofInstant(startedAt, ZoneOffset.UTC),
        null,
        Set.of(TestIds.district(1), TestIds.district(2)));
  }

  public static UserView author() {
    return new UserView(AUTHOR_ID, "Nimal Perera", Role.DMC_OFFICER, null, null, null, null);
  }

  /** A saved report with the given section data (keyed by SectionKey name) and unavailable list. */
  public static DisasterReport report(
      Map<String, Object> sections, List<Map<String, String>> unavailable) {
    return new DisasterReport(
        EVENT_ID,
        Map.of("districtIds", List.of(RATNAPURA.toString())),
        sections,
        unavailable,
        AUTHOR_ID,
        GENERATED_AT);
  }

  public static DisasterReport emptyReport() {
    return report(Map.of(), List.of());
  }
}
