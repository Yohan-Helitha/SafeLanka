package lk.dmc.disaster.auth.domain;

import java.util.Optional;
import java.util.regex.Pattern;

/** A Sri Lankan mobile number, always held in E.164 form ({@code +947XXXXXXXX}). */
public record PhoneNumber(String e164) {

  /** Accepts 07XXXXXXXX, 7XXXXXXXX and +947XXXXXXXX (spaces and dashes are ignored). */
  public static final String PATTERN = "^(?:\\+94|0)?7\\d{8}$";

  private static final Pattern VALID = Pattern.compile(PATTERN);

  /** Parses user input, or returns empty when it is not a Sri Lankan mobile number. */
  public static Optional<PhoneNumber> parse(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String compact = input.replaceAll("[\\s-]", "");
    if (!VALID.matcher(compact).matches()) {
      return Optional.empty();
    }
    String nineDigits = compact.substring(compact.length() - 9);
    return Optional.of(new PhoneNumber("+94" + nineDigits));
  }

  /** "+94 77 *** 4567": enough for the person to recognise it, not enough to leak it. */
  public String masked() {
    String d = e164.substring(3);
    return "+94 " + d.substring(0, 2) + " *** " + d.substring(5);
  }
}
