
package com.ejada.moneytime.starter.validation;

import com.ejada.moneytime.starter.money.MoneyUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;

public class MoneyMaxValidator implements ConstraintValidator<MoneyMax, MonetaryAmount> {
  private BigDecimal max;
  @Override
  public void initialize(MoneyMax constraintAnnotation) {
    max = new BigDecimal(constraintAnnotation.value());
  }

  @Override
  public boolean isValid(MonetaryAmount value, ConstraintValidatorContext context) {
    if (value == null) return true;
    return MoneyUtils.amount(value).compareTo(max) <= 0;
  }
}
