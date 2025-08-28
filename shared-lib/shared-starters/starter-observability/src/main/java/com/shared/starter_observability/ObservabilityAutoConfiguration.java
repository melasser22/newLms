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
 @Bean
    OpenTelemetry openTelemetry(ObservabilityProps props) {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, props.getApplication())));
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                .setResource(resource)
                .build();
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    }

    @Bean
    Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("shared-lib");
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
