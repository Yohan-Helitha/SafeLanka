package lk.dmc.disaster.shared.error;

import java.util.LinkedHashMap;
import java.util.Map;
import lk.dmc.disaster.shared.api.ErrorBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns every failure into the {@code { "error": { code, message, details } }} envelope. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(AppException.class)
  ResponseEntity<ErrorBody> handleApp(AppException e) {
    return respond(e.code(), e.getMessage(), e.details());
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorBody> handleAuthentication(AuthenticationException e) {
    return respond(ErrorCode.UNAUTHENTICATED, "Log in to continue.", Map.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorBody> handleDenied(AccessDeniedException e) {
    return respond(ErrorCode.FORBIDDEN_ROLE, "Your role cannot do this.", Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> handleUnexpected(Exception e) {
    log.error("Unhandled exception", e);
    return respond(
        ErrorCode.INTERNAL_ERROR,
        "Something went wrong on our side. Try again in a moment.",
        Map.of());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, Object> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    ErrorBody body =
        ErrorBody.of(
            ErrorCode.VALIDATION_ERROR.name(),
            "Some details need fixing.",
            Map.of("fields", fields));
    return ResponseEntity.status(HttpStatusCode.valueOf(400)).body(body);
  }

  /**
   * Spring MVC's own errors (malformed JSON, missing parameter, 404, 405...) share the envelope.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ErrorCode code =
        status.value() == 404
            ? ErrorCode.NOT_FOUND
            : status.is4xxClientError() ? ErrorCode.VALIDATION_ERROR : ErrorCode.INTERNAL_ERROR;
    String message =
        code == ErrorCode.NOT_FOUND
            ? "That was not found."
            : code == ErrorCode.VALIDATION_ERROR
                ? "The request could not be understood."
                : "Something went wrong on our side. Try again in a moment.";
    return ResponseEntity.status(status)
        .headers(headers)
        .body(ErrorBody.of(code.name(), message, Map.of()));
  }

  private static ResponseEntity<ErrorBody> respond(
      ErrorCode code, String message, Map<String, Object> details) {
    return ResponseEntity.status(code.status()).body(ErrorBody.of(code.name(), message, details));
  }
}
