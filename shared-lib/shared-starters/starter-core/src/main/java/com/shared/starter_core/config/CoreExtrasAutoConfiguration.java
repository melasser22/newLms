package com.shared.starter_core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({EnvironmentLogger.class, PerformanceConfig.class})
public class CoreExtrasAutoConfiguration {
}
