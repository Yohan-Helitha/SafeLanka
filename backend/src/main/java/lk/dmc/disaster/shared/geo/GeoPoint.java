package lk.dmc.disaster.shared.geo;

import lk.dmc.disaster.shared.error.BusinessRuleException;

/** A coordinate pair known to lie inside Sri Lanka. Build it with {@link #of}. */
public record GeoPoint(double latitude, double longitude) {

  public static final double MIN_LATITUDE = 5.8;
  public static final double MAX_LATITUDE = 9.9;
  public static final double MIN_LONGITUDE = 79.5;
  public static final double MAX_LONGITUDE = 82.0;

  /** Rejects coordinates outside Sri Lanka (and NaN) with a 422. */
  public static GeoPoint of(double latitude, double longitude) {
    boolean inside =
        latitude >= MIN_LATITUDE
            && latitude <= MAX_LATITUDE
            && longitude >= MIN_LONGITUDE
            && longitude <= MAX_LONGITUDE;
    if (!inside) {
      throw new BusinessRuleException("The location must be inside Sri Lanka.");
    }
    return new GeoPoint(latitude, longitude);
  }
}
