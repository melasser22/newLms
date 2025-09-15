package com.ejada.shared_starter_validation.constraints;
import jakarta.validation.ConstraintValidator; import jakarta.validation.ConstraintValidatorContext;
public class TrimmedValidator implements ConstraintValidator<Trimmed, String> {
  @Override public boolean isValid(String v, ConstraintValidatorContext c){ return v==null || v.equals(v.trim()); }
}
