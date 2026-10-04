package lk.dmc.disaster.auth.domain;

import java.util.Locale;

/** A Sri Lankan national identity card number, stored in upper case. */
public final class Nic {

  /** Old format (9 digits and V or X) or new format (12 digits). */
  public static final String PATTERN = "^(?:\\d{9}[VvXx]|\\d{12})$";

  private Nic() {}

  public static String normalise(String input) {
    return input.trim().toUpperCase(Locale.ROOT);
  }
}
