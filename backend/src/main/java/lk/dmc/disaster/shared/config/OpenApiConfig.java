package lk.dmc.disaster.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "BearerAuth";
  private static final String DEMO_ACTING_USER = "DemoActingUser";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("SafeLanka Disaster API")
            .description("Disaster early warning and emergency response coordination API")
            .version("v1"))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH).addList(DEMO_ACTING_USER))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your Bearer JWT token from /api/auth/login"))
            .addSecuritySchemes(DEMO_ACTING_USER, new SecurityScheme()
                .name("X-Acting-User")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .description("Demo profile only: user UUID (e.g. 00000000-0000-0000-0002-000000000001 for District Officer)")));
  }
}
