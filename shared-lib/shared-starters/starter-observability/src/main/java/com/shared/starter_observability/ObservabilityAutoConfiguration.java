package com.shared.starter_observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that wires basic Micrometer and OpenTelemetry components
 * with sensible defaults for Shared services.
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, ObservationRegistry.class})
@EnableConfigurationProperties(ObservabilityAutoConfiguration.ObservabilityProps.class)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTagsCustomizer(ObservabilityProps props) {
        return registry -> registry.config().commonTags("application", props.getApplication());
    }

    /**
     * Externalized configuration properties for observability features.
     */
    @ConfigurationProperties("shared.observability")
    public static class ObservabilityProps {
        /** Application tag applied to all exported metrics. */
        private String application = "app";

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }
    }
}
