package lk.dmc.disaster.auth.domain;

/** Password rule: 8 to 64 characters with at least one letter and one number. */
public final class PasswordPolicy {

  public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
  public static final String MESSAGE =
      "Use 8 to 64 characters with at least one letter and one number.";

  private PasswordPolicy() {}

  public static boolean isValid(String password) {
    return password != null && password.matches(PATTERN);
  }
}
