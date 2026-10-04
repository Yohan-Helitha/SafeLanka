package lk.dmc.disaster.auth.security;

import lk.dmc.disaster.shared.actor.ActingUserRegistrar;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * The real security setup: stateless, every request carries a bearer JWT. CSRF protection is off
 * because no cookie authenticates requests (the refresh cookie is SameSite=Strict and only sent to
 * {@code /api/auth}).
 */
@Configuration
@EnableWebSecurity
@Profile("!demo-auth")
class JwtSecurityConfig {

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http,
      ActingUserRegistrar registrar,
      JwtAuthenticationConverter converter,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
      throws Exception {
    var entryPoint = RestSecurityHandlers.entryPoint(resolver);
    var accessDenied = RestSecurityHandlers.accessDenied(resolver);

    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(AccessRules::apply)
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDenied))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
        .addFilterAfter(new JwtActingUserFilter(registrar), BearerTokenAuthenticationFilter.class);
    return http.build();
  }
}
