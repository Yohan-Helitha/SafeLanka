package lk.dmc.disaster.analytics.entity;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

/** The numbers and patterns behind the UC04 rules, in one place. */
public final class AnalyticsRules {

  /**
   * A report window may start or end this far outside the event. The date pickers only have minute
   * precision, so an event that started at 00:30:30 would otherwise reject its own start time.
   */
  public static final Duration WINDOW_TOLERANCE = Duration.ofMinutes(1);

  /** Export file names look like {@code disaster-report-kalu-flood-may-2026-20261004.pdf}. */
  public static final String EXPORT_FILE_PREFIX = "disaster-report-";

  public static final DateTimeFormatter EXPORT_FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  /** The text an exporter prints for a section that has no data. */
  public static final String DATA_UNAVAILABLE = "Data unavailable";

  private AnalyticsRules() {}
}
