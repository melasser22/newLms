package com.ejada.starter_core.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Registers {@link NoResourceFoundExceptionHandler} only when the reactive
 * {@code NoResourceFoundException} is present on the classpath.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.reactive.resource.NoResourceFoundException")
@Import(NoResourceFoundExceptionHandler.class)
public class NoResourceFoundHandlerConfiguration {
}
