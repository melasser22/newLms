package com.shared.shared_starter_validation.constraints;
import jakarta.validation.ConstraintValidator; import jakarta.validation.ConstraintValidatorContext;
import java.util.Currency;
public class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {
  @Override public boolean isValid(String v, ConstraintValidatorContext c){
    if (v==null) return true;
    try { Currency.getInstance(v); return true; } catch (Exception e){ return false; }
  }
}
