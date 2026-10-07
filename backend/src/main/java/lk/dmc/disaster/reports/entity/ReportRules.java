package lk.dmc.disaster.reports.entity;

import java.time.Duration;

/** The numbers behind the UC02 business rules, in one place. */
public final class ReportRules {

  public static final int DESCRIPTION_MIN = 10;
  public static final int DESCRIPTION_MAX = 500;
  public static final int COMMENT_MAX = 300;
  public static final int REQUEST_INFO_COMMENT_MIN = 5;
  public static final int MANUAL_LOCATION_MIN = 5;
  public static final int MANUAL_LOCATION_MAX = 200;
  public static final int PHOTO_MAX_BYTES = 5 * 1024 * 1024;
  public static final double DUPLICATE_RADIUS_METRES = 500;
  public static final Duration DUPLICATE_WINDOW = Duration.ofHours(2);
  public static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
  public static final String REFERENCE_FORMAT = "RPT-%d-%04d";

  private ReportRules() {}
}
