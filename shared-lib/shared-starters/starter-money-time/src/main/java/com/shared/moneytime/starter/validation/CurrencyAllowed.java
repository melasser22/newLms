
package com.shared.moneytime.starter.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CurrencyAllowedValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrencyAllowed {
  String message() default "currency not allowed";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  String[] value();
}
