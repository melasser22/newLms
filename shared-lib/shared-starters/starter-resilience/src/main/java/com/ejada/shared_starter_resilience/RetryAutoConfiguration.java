package com.ejada.shared_starter_resilience;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Auto-configuration that switches on Spring Retry annotations when the
 * {@code spring-retry} module is present. This allows services consuming the
 * shared starter to declaratively use {@link org.springframework.retry.annotation.Retryable}
 * without having to manually add {@link EnableRetry} in every application.
 */
@AutoConfiguration
@ConditionalOnClass(EnableRetry.class)
@EnableRetry
public class RetryAutoConfiguration {
}
