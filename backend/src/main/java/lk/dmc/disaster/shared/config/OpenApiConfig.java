package lk.dmc.disaster.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI title and the two ways to authenticate: the JWT bearer token, and (demo profile only)
 * the {@code X-Acting-User} header. The per-module groups are plain configuration under {@code
 * springdoc.group-configs} in application.yml, so a new module never edits this class.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";
  private static final String DEMO_ACTING_USER = "demoActingUser";

  @Bean
  OpenAPI lankaGuardOpenApi() {
    SecurityScheme bearerJwt =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Access token from POST /api/auth/login");
    SecurityScheme demoHeader =
        new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-Acting-User")
            .description("Demo profile only: the id of the user to act as");
    return new OpenAPI()
        .info(
            new Info()
                .title("LankaGuard API")
                .description("Disaster early warning and emergency response coordination API")
                .version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(BEARER, bearerJwt)
                .addSecuritySchemes(DEMO_ACTING_USER, demoHeader))
        // Two separate requirements: either one authenticates a request.
        .addSecurityItem(new SecurityRequirement().addList(BEARER))
        .addSecurityItem(new SecurityRequirement().addList(DEMO_ACTING_USER));
  }
}
