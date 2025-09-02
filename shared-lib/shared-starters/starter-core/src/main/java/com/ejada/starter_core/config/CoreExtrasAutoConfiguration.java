package com.ejada.starter_core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(EnvironmentLogger.class)
public class CoreExtrasAutoConfiguration {
}
