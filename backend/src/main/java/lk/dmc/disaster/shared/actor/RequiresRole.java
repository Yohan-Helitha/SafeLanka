package lk.dmc.disaster.shared.actor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lk.dmc.disaster.shared.domain.Role;

/**
 * Restricts a controller (or one of its methods) to the listed roles. Checked by {@link
 * RoleInterceptor}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresRole {

  Role[] value();
}
