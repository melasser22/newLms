package com.ejada.config.validation;

import java.util.Locale;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Base class for configuration validators that provides helper methods for
 * common error-reporting behaviour while delegating the actual validation logic
 * to subclasses.
 *
 * @param <T> type of configuration properties being validated
 */
public abstract class ConfigurationValidator<T> implements Validator {

  private final Class<T> targetType;

  protected ConfigurationValidator(Class<T> targetType) {
    this.targetType = targetType;
  }

  @Override
  public final boolean supports(Class<?> clazz) {
    return targetType.isAssignableFrom(clazz);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final void validate(Object target, Errors errors) {
    if (target == null) {
      reject(errors, "configuration.missing", "Configuration properties instance must not be null");
      return;
    }
    doValidate((T) target, errors);
  }

  /**
   * Implementor-specific validation logic.
   *
   * @param target bound configuration properties
   * @param errors binding errors collector
   */
  protected abstract void doValidate(T target, Errors errors);

  protected void reject(Errors errors, String code, String defaultMessage, Object... args) {
    errors.reject(code, args, defaultMessage);
  }

  protected void rejectValue(Errors errors, String field, String code, String defaultMessage, Object... args) {
    errors.rejectValue(field, code, args, defaultMessage);
  }

  protected String maskedValue(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    if (trimmed.length() <= 4) {
      return "****";
    }
    return trimmed.substring(0, 2) + "****" + trimmed.substring(trimmed.length() - 2);
  }

  protected String normalizeKey(String key) {
    return (key == null) ? "" : key.toLowerCase(Locale.ROOT);
  }
}
