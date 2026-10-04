package lk.dmc.disaster.shared.api;

import java.util.Map;

/** Error envelope: {@code { "error": { "code", "message", "details" } }}. */
public record ErrorBody(Error error) {

  public record Error(String code, String message, Map<String, Object> details) {}

  public static ErrorBody of(String code, String message, Map<String, Object> details) {
    return new ErrorBody(new Error(code, message, details));
  }
}
