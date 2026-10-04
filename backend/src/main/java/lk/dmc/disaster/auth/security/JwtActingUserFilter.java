package lk.dmc.disaster.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lk.dmc.disaster.shared.actor.ActingUserRegistrar;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bridges Spring Security to the rest of the app: once a valid token is accepted, the same
 * request-scoped {@code ActingUser} that module code reads is filled from the token's claims.
 * Created by the security config, not a bean, so the servlet container does not register it twice.
 */
class JwtActingUserFilter extends OncePerRequestFilter {

  private final ActingUserRegistrar registrar;

  JwtActingUserFilter(ActingUserRegistrar registrar) {
    this.registrar = registrar;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken token) {
      registrar.register(request, JwtClaims.toActingUser(token.getToken()));
    }
    chain.doFilter(request, response);
  }
}
