
package com.ejada.moneytime.starter.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CurrencyAllowedValidator implements ConstraintValidator<CurrencyAllowed, String> {
  private Set<String> allowed;

  @Override
  public void initialize(CurrencyAllowed constraintAnnotation) {
    allowed = new HashSet<>();
    Arrays.stream(constraintAnnotation.value())
        .map(String::toUpperCase)
        .forEach(allowed::add);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) return true;
    return allowed.contains(value.toUpperCase(Locale.ROOT));
  }
}
