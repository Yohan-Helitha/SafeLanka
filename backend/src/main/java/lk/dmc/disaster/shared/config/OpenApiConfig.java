package lk.dmc.disaster.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI title and JWT bearer scheme. The per-module groups are plain configuration under
 * {@code springdoc.group-configs} in application.yml, so a new module never edits this class.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  OpenAPI lankaGuardOpenApi() {
    SecurityScheme bearerJwt =
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");
    return new OpenAPI()
        .info(new Info().title("LankaGuard API").version("v1"))
        .components(new Components().addSecuritySchemes(BEARER, bearerJwt))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
