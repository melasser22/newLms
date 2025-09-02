
package com.ejada.moneytime.starter.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MoneyMinValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MoneyMin {
  String message() default "amount is below minimum";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  String value();     // numeric string like "0.00"
}
