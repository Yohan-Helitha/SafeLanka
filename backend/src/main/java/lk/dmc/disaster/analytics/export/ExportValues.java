package lk.dmc.disaster.analytics.export;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads values out of a saved report. Sections are stored as JSON, so what comes back is plain maps
 * and lists, and a time may be an ISO string or a number of epoch seconds.
 */
final class ExportValues {

  private ExportValues() {}

  static Map<?, ?> map(Object value) {
    return value instanceof Map<?, ?> m ? m : Map.of();
  }

  static List<?> list(Object value) {
    return value instanceof List<?> l ? l : List.of();
  }

  /** The text of a value, or an empty string for null. */
  static String text(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  static long number(Object value) {
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(text(value));
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  static double decimal(Object value) {
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(text(value));
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  /** A stored time as an {@link Instant}; empty when it is missing or not a time. */
  static Optional<Instant> instant(Object value) {
    if (value == null) {
      return Optional.empty();
    }
    try {
      if (value instanceof Number n) {
        return Optional.of(fromEpoch(n.doubleValue()));
      }
      String s = String.valueOf(value);
      if (s.matches("[0-9]+([.][0-9]+)?")) {
        return Optional.of(fromEpoch(Double.parseDouble(s)));
      }
      return Optional.of(Instant.parse(s));
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  /** Epoch seconds (with a fraction) or epoch milliseconds. */
  private static Instant fromEpoch(double v) {
    return v > 1_000_000_000_000L ? Instant.ofEpochMilli((long) v) : Instant.ofEpochMilli((long) (v * 1000));
  }
}
