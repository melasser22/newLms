package com.ejada.config.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Meta-annotation that marks a Spring {@link org.springframework.validation.Validator}
 * as a configuration properties validator. Validators annotated with this are
 * automatically picked up by the {@link org.springframework.boot.context.properties.bind.Binder}
 * during property binding.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationPropertiesBinding
@Component
@Documented
public @interface ConfigurationPropertiesValidator {

  /**
   * Alias for the underlying component name to ease bean registration when used on methods.
   */
  @AliasFor(annotation = Component.class, attribute = "value")
  String value() default "";
}
