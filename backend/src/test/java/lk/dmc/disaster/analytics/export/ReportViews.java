package lk.dmc.disaster.analytics.export;

import static lk.dmc.disaster.analytics.AnalyticsFixtures.EVENT_ID;
import static lk.dmc.disaster.analytics.AnalyticsFixtures.GENERATED_AT;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Report views shaped like saved reports: sections as plain JSON-style maps, times as numbers. */
final class ReportViews {

  static final UUID SHELTER = UUID.fromString("00000000-0000-0000-0008-000000000001");

  private ReportViews() {}

  /** Epoch seconds with a fraction, which is how a saved Instant comes back from JSONB. */
  static double epoch(String iso) {
    return Instant.parse(iso).getEpochSecond() + 0.0;
  }

  static DisasterReportView of(
      Map<String, Object> sections, List<Map<String, String>> unavailable) {
    return new DisasterReportView(
        UUID.randomUUID(),
        EVENT_ID,
        "Kalu Flood May 2026",
        Map.of("districtIds", List.of("d1", "d2"), "from", "2026-05-14T00:10:00Z", "to", "2026-05-16T00:00:00Z"),
        sections,
        unavailable,
        "Nimal Perera",
        GENERATED_AT);
  }

  static Map<String, Object> alertTimeline() {
    return Map.of(
        "firstWarningAt", epoch("2026-05-14T01:30:00Z"),
        "firstVerifiedReportAt", "2026-05-14T00:05:00Z",
        "reportToWarningMinutes", 85L,
        "entries",
            List.of(
                Map.of(
                    "warningId", "w1", "level", "WATCH", "status", "ESCALATED",
                    "issuedAt", epoch("2026-05-14T01:30:00Z")),
                Map.of(
                    "warningId", "w2", "level", "WARNING", "status", "ESCALATED",
                    "issuedAt", epoch("2026-05-14T20:30:00Z"), "supersedesId", "w1")));
  }

  static Map<String, Object> citizensReached() {
    return Map.of(
        "uniqueCitizensTargeted", 17L,
        "uniqueCitizensReached", 17L,
        "deliveryRate", 1.0,
        "byChannel", List.of(Map.of("channel", "SMS", "delivered", 47L, "failed", 4L)),
        "byDistrict", List.of(Map.of("districtName", "Ratnapura", "targeted", 8L, "reached", 8L)));
  }

  static Map<String, Object> shelterOccupancy() {
    return Map.of(
        "series",
            List.of(
                Map.of(
                    "shelterId", SHELTER.toString(),
                    "shelterName", "Sivali, Central College",
                    "capacity", 300L,
                    "points", List.of(Map.of("recordedAt", epoch("2026-05-14T22:00:00Z"), "occupancy", 20L)))),
        "peaks",
            List.of(
                Map.of(
                    "shelterId", SHELTER.toString(), "peakOccupancy", 290L, "capacity", 300L,
                    "peakRatio", 0.97, "peakAt", epoch("2026-05-16T04:00:00Z"))));
  }

  static Map<String, Object> resourceDistribution() {
    return Map.of(
        "byDistrict",
            List.of(
                Map.of(
                    "districtName", "Ratnapura", "itemCode", "DRY_RATION", "unit", "packs",
                    "allocated", 250L, "distributed", 250L)),
        "byOrganisationType", List.of(Map.of("type", "GOVERNMENT", "distributed", 250L)),
        "byItem", List.of(Map.of("itemCode", "DRY_RATION", "unit", "packs", "distributed", 250L)));
  }

  static Map<String, Object> allSections() {
    return Map.of(
        "ALERT_TIMELINE", alertTimeline(),
        "CITIZENS_REACHED", citizensReached(),
        "SHELTER_OCCUPANCY", shelterOccupancy(),
        "RESOURCE_DISTRIBUTION", resourceDistribution());
  }
}
