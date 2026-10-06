package lk.dmc.disaster.response.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PriorityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPriority {

  String message() default "Priority must be 1 (High), 2 (Medium), or 3 (Low)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
