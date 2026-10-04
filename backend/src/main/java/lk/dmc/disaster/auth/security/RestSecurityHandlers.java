package lk.dmc.disaster.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Security failures happen in filters, before MVC. These hand them to the MVC exception resolver so
 * they come back in the same JSON error envelope as every other error.
 */
final class RestSecurityHandlers {

  private RestSecurityHandlers() {}

  static AuthenticationEntryPoint entryPoint(HandlerExceptionResolver resolver) {
    return (HttpServletRequest request, HttpServletResponse response, AuthenticationException e) ->
        resolver.resolveException(
            request,
            response,
            null,
            new AppException(ErrorCode.UNAUTHENTICATED, "Log in to continue."));
  }

  static AccessDeniedHandler accessDenied(HandlerExceptionResolver resolver) {
    return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) ->
        resolver.resolveException(
            request,
            response,
            null,
            new AppException(ErrorCode.FORBIDDEN_ROLE, "Your role cannot do this."));
  }
}
