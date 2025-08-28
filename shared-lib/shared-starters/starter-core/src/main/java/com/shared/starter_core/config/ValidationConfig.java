package com.shared.starter_core.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfig {

    /**
     * Standard Spring Validator bean that integrates Hibernate Validator.
     * This makes @Valid and @Validated annotations work everywhere.
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Expose the Jakarta Validator explicitly (optional but useful if you want
     * to inject Validator into services directly).
     */
    @Bean
    public Validator jakartaValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }
}
