package lk.dmc.disaster.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lk.dmc.disaster.auth.domain.UserAccount;
import lk.dmc.disaster.auth.persistence.UserAccountRepository;
import lk.dmc.disaster.shared.actor.ActingUser;
import lk.dmc.disaster.shared.actor.ActingUserRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Demo fallback (profile {@code demo-auth}): no login, the frontend's role switcher sends {@code
 * X-Acting-User}. Never enable this outside a demo. {@code @RequiresRole} still applies.
 */
@Configuration
@EnableWebSecurity
@Profile("demo-auth")
class DemoSecurityConfig {

  @Bean
  SecurityFilterChain demoFilterChain(
      HttpSecurity http, ActingUserRegistrar registrar, UserAccountRepository users)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .addFilterBefore(
            new HeaderActingUserFilter(registrar, users),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  static class HeaderActingUserFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Acting-User";

    private final ActingUserRegistrar registrar;
    private final UserAccountRepository users;

    HeaderActingUserFilter(ActingUserRegistrar registrar, UserAccountRepository users) {
      this.registrar = registrar;
      this.users = users;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      String header = request.getHeader(HEADER);
      if (header != null) {
        try {
          users
              .findById(UUID.fromString(header))
              .map(HeaderActingUserFilter::toActingUser)
              .ifPresent(user -> registrar.register(request, user));
        } catch (IllegalArgumentException ignored) {
          // Not a UUID: treated as anonymous.
        }
      }
      chain.doFilter(request, response);
    }

    private static ActingUser toActingUser(UserAccount u) {
      return new ActingUser(
          u.getId(), u.getRole(), u.getDistrictId(), u.getRiverBasinId(), u.getRescueTeamId());
    }
  }
}
