package com.ejada.gateway.config.validation;

import com.ejada.config.validation.ConfigurationPropertiesValidator;
import com.ejada.config.validation.ConfigurationValidator;
import com.ejada.gateway.config.GatewayWebClientProperties;
import java.time.Duration;
import org.springframework.validation.Errors;

/**
 * Ensures gateway web client timeouts remain within sane operational bounds so
 * misconfigured values do not result in excessive retry storms or overly short
 * cancellation windows.
 */
@ConfigurationPropertiesValidator
public class GatewayWebClientPropertiesValidator extends ConfigurationValidator<GatewayWebClientProperties> {

  private static final Duration MIN_TIMEOUT = Duration.ofMillis(50);
  private static final Duration MAX_TIMEOUT = Duration.ofMinutes(5);

  public GatewayWebClientPropertiesValidator() {
    super(GatewayWebClientProperties.class);
  }

  @Override
  protected void doValidate(GatewayWebClientProperties target, Errors errors) {
    validateDuration(errors, "connectTimeout", target.getConnectTimeout());
    validateDuration(errors, "responseTimeout", target.getResponseTimeout());
    validateDuration(errors, "readTimeout", target.getReadTimeout());
    validateDuration(errors, "writeTimeout", target.getWriteTimeout());
    if (target.getMaxInMemorySize() < 1024) {
      rejectValue(errors, "maxInMemorySize", "gateway.webclient.buffer.too-small",
          "Max in-memory size must be at least 1KB");
    }
  }

  private void validateDuration(Errors errors, String field, Duration value) {
    if (value == null) {
      return;
    }
    if (value.isNegative() || value.isZero() || value.minus(MIN_TIMEOUT).isNegative()) {
      rejectValue(errors, field, "gateway.webclient.timeout.too-small",
          "Timeout %s must be greater than %s", field, MIN_TIMEOUT);
      return;
    }
    if (value.compareTo(MAX_TIMEOUT) > 0) {
      rejectValue(errors, field, "gateway.webclient.timeout.too-large",
          "Timeout %s must be less than %s", field, MAX_TIMEOUT);
    }
  }
}
