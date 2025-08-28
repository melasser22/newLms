
package com.shared.moneytime.starter.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MoneyMaxValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MoneyMax {
  String message() default "amount is above maximum";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  String value();   // numeric string
}
