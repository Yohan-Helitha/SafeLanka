package lk.dmc.disaster.shared.error;

/** The request clashes with current state (409). Use a specific 409 code when one fits. */
public class ConflictException extends AppException {

  public ConflictException(String message) {
    this(ErrorCode.CONFLICT, message);
  }

  public ConflictException(ErrorCode code, String message) {
    super(requireConflict(code), message);
  }

  private static ErrorCode requireConflict(ErrorCode code) {
    if (code.status().value() != 409) {
      throw new IllegalArgumentException(code + " is not a 409 error code");
    }
    return code;
  }
}
