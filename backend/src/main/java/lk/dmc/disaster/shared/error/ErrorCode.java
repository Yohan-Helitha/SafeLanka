package lk.dmc.disaster.shared.error;

import org.springframework.http.HttpStatus;

/** Machine-readable error codes returned to clients, each tied to one HTTP status. */
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
  FORBIDDEN_ROLE(HttpStatus.FORBIDDEN),
  PHONE_NOT_VERIFIED(HttpStatus.FORBIDDEN),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  CONFLICT(HttpStatus.CONFLICT),
  INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
  INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
  CAPACITY_EXCEEDED(HttpStatus.CONFLICT),
  TEAM_NOT_AVAILABLE(HttpStatus.CONFLICT),
  CODE_INVALID(HttpStatus.BAD_REQUEST),
  CODE_EXPIRED(HttpStatus.GONE),
  ACCOUNT_LOCKED(HttpStatus.LOCKED),
  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
  SMS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
  BUSINESS_RULE(HttpStatus.UNPROCESSABLE_CONTENT),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

  ErrorCode(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
