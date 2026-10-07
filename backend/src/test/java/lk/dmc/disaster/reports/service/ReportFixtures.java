package lk.dmc.disaster.reports.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.ReportDraft;

/** Builds reports for service tests without a database. */
public final class ReportFixtures {

  public static final Instant NOW = Instant.parse("2026-10-04T08:10:02Z");
  public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  public static final UUID FLOOD = UUID.randomUUID();
  public static final UUID DISTRICT = UUID.randomUUID();

  private ReportFixtures() {}

  public static HazardReport gps(UUID reporter, double lat, double lng, Instant capturedAt) {
    return report(reporter, FLOOD, lat, lng, null, capturedAt);
  }

  public static HazardReport manual(UUID reporter, Instant capturedAt) {
    return report(reporter, FLOOD, null, null, "Next to the old railway bridge", capturedAt);
  }

  public static HazardReport report(
      UUID reporter,
      UUID hazardType,
      Double lat,
      Double lng,
      String manualText,
      Instant capturedAt) {
    return HazardReport.submit(
        new ReportDraft(
            UUID.randomUUID(),
            hazardType,
            "RISING_WATER",
            "Water over the road near Kolonnawa canal bridge",
            lat,
            lng,
            manualText,
            DISTRICT,
            capturedAt),
        reporter,
        "RPT-2026-" + UUID.randomUUID().toString().substring(0, 4),
        CLOCK);
  }
}
