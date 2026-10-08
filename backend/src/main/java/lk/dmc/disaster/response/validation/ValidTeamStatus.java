package lk.dmc.disaster.response.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TeamStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTeamStatus {

  String message() default
      "Invalid team status. Must be one of: AVAILABLE, DISPATCHED, EN_ROUTE, ACTIVE, OFFLINE_UNKNOWN";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
