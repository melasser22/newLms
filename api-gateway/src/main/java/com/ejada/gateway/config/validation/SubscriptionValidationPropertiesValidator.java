package com.ejada.gateway.config.validation;

import com.ejada.config.validation.ConfigurationPropertiesValidator;
import com.ejada.config.validation.ConfigurationValidator;
import com.ejada.gateway.config.SubscriptionValidationProperties;
import java.net.URI;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Guards the subscription validation settings to ensure cache TTLs and
 * skip-patterns are sensible before the filter activates.
 */
@ConfigurationPropertiesValidator
public class SubscriptionValidationPropertiesValidator
    extends ConfigurationValidator<SubscriptionValidationProperties> {

  private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

  public SubscriptionValidationPropertiesValidator() {
    super(SubscriptionValidationProperties.class);
  }

  @Override
  protected void doValidate(SubscriptionValidationProperties properties, Errors errors) {
    if (properties.getCacheTtl() != null && properties.getCacheTtl().isNegative()) {
      rejectValue(errors, "cacheTtl", "gateway.subscription.cache.negative", "Cache TTL must not be negative");
    }
    if (!StringUtils.hasText(properties.getValidationUri())) {
      rejectValue(errors, "validationUri", "gateway.subscription.validation-uri.blank",
          "Validation URI must not be blank");
    } else {
      try {
        URI uri = URI.create(properties.getValidationUri());
        if (!StringUtils.hasText(uri.getScheme())) {
          rejectValue(errors, "validationUri", "gateway.subscription.validation-uri.scheme",
              "Validation URI must include a scheme");
        }
      } catch (IllegalArgumentException ex) {
        rejectValue(errors, "validationUri", "gateway.subscription.validation-uri.invalid",
            "Validation URI is not a valid URI: %s", properties.getValidationUri());
      }
    }
    String[] skipPatterns = properties.getSkipPatterns();
    if (skipPatterns != null) {
      for (int i = 0; i < skipPatterns.length; i++) {
        String candidate = skipPatterns[i];
        if (!StringUtils.hasText(candidate)) {
          rejectValue(errors, "skipPatterns[" + i + "]", "gateway.subscription.skip.blank",
              "Skip pattern index %d must not be blank", i);
          continue;
        }
        try {
          PATH_PATTERN_PARSER.parse(candidate);
        } catch (IllegalArgumentException ex) {
          rejectValue(errors, "skipPatterns[" + i + "]", "gateway.subscription.skip.invalid",
              "Skip pattern '%s' is not a valid Spring path expression", candidate);
        }
      }
    }
  }
}
