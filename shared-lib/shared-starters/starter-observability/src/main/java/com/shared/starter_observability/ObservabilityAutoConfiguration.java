package com.shared.starter_observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Basic observability auto-configuration that:
 *  - Applies a common "application" tag to all Micrometer meters
 *  - Exposes an OpenTelemetry {@link Tracer} backed by the global instance
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityAutoConfiguration.ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTagsCustomizer(ObservabilityProperties props) {
        return registry -> registry.config().commonTags("application", props.getApplicationName());
    }

    @Bean
    public Tracer tracer() {
        return GlobalOpenTelemetry.getTracer("shared-starter");
    }

    @ConfigurationProperties(prefix = "shared.observability")
    public static class ObservabilityProperties {
        /** Application name used for tagging metrics */
        private String applicationName = "app";

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }
    }
}
