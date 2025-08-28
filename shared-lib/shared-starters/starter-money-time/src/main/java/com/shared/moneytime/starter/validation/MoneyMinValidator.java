
package com.shared.moneytime.starter.validation;

import com.shared.moneytime.starter.money.MoneyUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;

public class MoneyMinValidator implements ConstraintValidator<MoneyMin, MonetaryAmount> {
  private BigDecimal min;
  @Override
  public void initialize(MoneyMin constraintAnnotation) {
    min = new BigDecimal(constraintAnnotation.value());
  }

  @Override
  public boolean isValid(MonetaryAmount value, ConstraintValidatorContext context) {
    if (value == null) return true;
    return MoneyUtils.amount(value).compareTo(min) >= 0;
  }
}
