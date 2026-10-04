package lk.dmc.disaster.shared.actor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.dmc.disaster.shared.error.AppException;
import lk.dmc.disaster.shared.error.ErrorCode;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** The precise per-method role rule; URL rules in the security config are only the coarse gate. */
@Component
public class RoleInterceptor implements HandlerInterceptor {

  private final ActingUserContext actingUser;

  public RoleInterceptor(ActingUserContext actingUser) {
    this.actingUser = actingUser;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod method)) {
      return true;
    }
    RequiresRole rule =
        AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresRole.class);
    if (rule == null) {
      rule = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresRole.class);
    }
    if (rule == null) {
      return true;
    }
    ActingUser user = actingUser.require();
    if (!user.hasRole(rule.value())) {
      throw new AppException(ErrorCode.FORBIDDEN_ROLE, "Your role cannot do this.");
    }
    return true;
  }
}
