package lk.dmc.disaster.shared.config;

import lk.dmc.disaster.shared.actor.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers cross-cutting MVC pieces for every module. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final RoleInterceptor roleInterceptor;

  public WebConfig(RoleInterceptor roleInterceptor) {
    this.roleInterceptor = roleInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(roleInterceptor).addPathPatterns("/api/**");
  }
}
