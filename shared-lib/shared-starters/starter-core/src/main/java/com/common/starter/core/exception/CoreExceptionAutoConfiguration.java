package com.common.starter.core.exception;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration to register {@link GlobalExceptionHandler}.
 */
@Configuration(proxyBeanMethods = false)
@Import(GlobalExceptionHandler.class)
public class CoreExceptionAutoConfiguration {
}
