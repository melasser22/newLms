package com.ejada.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled operational tasks such as cache warming.
 */
@Configuration
@EnableScheduling
public class OperationalSchedulingConfiguration {
}
