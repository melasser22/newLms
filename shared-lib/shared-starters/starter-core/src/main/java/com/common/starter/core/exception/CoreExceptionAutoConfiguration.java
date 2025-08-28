package com.common.starter.core.exception;

import com.shared.starter_core.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration that registers the consolidated {@link GlobalExceptionHandler} from the
 * {@code web} package. This avoids duplicate handler definitions and ensures a single,
 * consistent exception mapping across applications.
 */
@Configuration(proxyBeanMethods = false)
@Import(GlobalExceptionHandler.class)
public class CoreExceptionAutoConfiguration {
}
