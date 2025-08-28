package com.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Simple configuration properties holding common environment details. These values can be shared
 * across services to avoid configuration sprawl.
 */
@Data
@ConfigurationProperties(prefix = "shared")
public class EnvironmentProperties {
    /** Current runtime environment (e.g., dev, prod) */
    private String environment = "local";

    /** Application version */
    private String version = "0.0.1";
}
