package lk.dmc.disaster.auth.security;

import lk.dmc.disaster.shared.domain.Role;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * The coarse access matrix by URL. {@code @RequiresRole} on controller methods is the precise rule
 * (for example "own report only"); this is the first gate, and it fails closed: anything not listed
 * needs a signed-in user.
 */
final class AccessRules {

  private static final String[] PUBLIC_DOCS = {
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**"
  };

  private AccessRules() {}

  static void apply(
      AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry rules) {
    rules
        // Public
        .requestMatchers(PUBLIC_DOCS)
        .permitAll()
        .requestMatchers(HttpMethod.GET, "/api/reference/**")
        .permitAll()
        .requestMatchers("/api/auth/me", "/api/auth/logout")
        .authenticated()
        .requestMatchers("/api/auth/**")
        .permitAll()
        // Reports: citizens submit, DMC officers decide
        .requestMatchers("/api/reports/**")
        .hasAnyRole(name(Role.CITIZEN), name(Role.VOLUNTEER), name(Role.DMC_OFFICER))
        // Warnings: the citizen feed, then DMC (district officers may read)
        .requestMatchers(HttpMethod.GET, "/api/warnings/active/mine")
        .hasAnyRole(name(Role.CITIZEN), name(Role.VOLUNTEER))
        .requestMatchers(HttpMethod.GET, "/api/warnings/**")
        .hasAnyRole(name(Role.DMC_OFFICER), name(Role.DISTRICT_OFFICER))
        .requestMatchers("/api/warnings/**", "/api/hazards/**", "/api/simulation/**")
        .hasRole(name(Role.DMC_OFFICER))
        // Response coordination: officers manage; rescue members and coordinators act on their own
        .requestMatchers(
            "/api/response/**",
            "/api/rescue-teams/**",
            "/api/assignments/**",
            "/api/shelters/**",
            "/api/relief-stocks/**",
            "/api/allocations/**")
        .hasAnyRole(
            name(Role.DISTRICT_OFFICER),
            name(Role.DMC_OFFICER),
            name(Role.RESCUE_MEMBER),
            name(Role.SHELTER_COORDINATOR))
        // Analytics: DMC generates, district officers read and export
        .requestMatchers(HttpMethod.GET, "/api/analytics/**")
        .hasAnyRole(name(Role.DMC_OFFICER), name(Role.DISTRICT_OFFICER))
        .requestMatchers("/api/analytics/**")
        .hasRole(name(Role.DMC_OFFICER))
        .anyRequest()
        .authenticated();
  }

  private static String name(Role role) {
    return role.name();
  }
}
