package lk.dmc.disaster.shared.error;

import java.util.Map;

/** Business or request failure that maps to a specific {@link ErrorCode} and HTTP status. */
public class AppException extends RuntimeException {

  private final ErrorCode code;
  private final transient Map<String, Object> details;

  public AppException(ErrorCode code, String message) {
    this(code, message, Map.of());
  }

  public AppException(ErrorCode code, String message, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.details = Map.copyOf(details);
  }

  public ErrorCode code() {
    return code;
  }

  public Map<String, Object> details() {
    return details;
  }
}
