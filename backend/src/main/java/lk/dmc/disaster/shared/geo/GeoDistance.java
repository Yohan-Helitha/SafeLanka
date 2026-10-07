package lk.dmc.disaster.shared.geo;

/** Great-circle (haversine) distance between two points. */
public final class GeoDistance {

  private static final double EARTH_RADIUS_METRES = 6_371_000;

  private GeoDistance() {}

  public static double metresBetween(GeoPoint a, GeoPoint b) {
    double dLat = Math.toRadians(b.latitude() - a.latitude());
    double dLng = Math.toRadians(b.longitude() - a.longitude());
    double h =
        Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(a.latitude()))
                * Math.cos(Math.toRadians(b.latitude()))
                * Math.pow(Math.sin(dLng / 2), 2);
    return 2 * EARTH_RADIUS_METRES * Math.asin(Math.sqrt(h));
  }
}
