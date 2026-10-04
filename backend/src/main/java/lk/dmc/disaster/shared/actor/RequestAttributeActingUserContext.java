package lk.dmc.disaster.shared.actor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Keeps the acting user in a request attribute, so it lives exactly as long as the request. */
@Component
class RequestAttributeActingUserContext implements ActingUserContext, ActingUserRegistrar {

  private static final String ATTRIBUTE = ActingUser.class.getName();

  @Override
  public void register(HttpServletRequest request, ActingUser user) {
    request.setAttribute(ATTRIBUTE, user);
  }

  @Override
  public Optional<ActingUser> current() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servlet) {
      return Optional.ofNullable((ActingUser) servlet.getRequest().getAttribute(ATTRIBUTE));
    }
    return Optional.empty();
  }
}
