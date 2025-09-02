package com.ejada.starter_observability;

import io.micrometer.core.instrument.MeterRegistry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;



import static org.assertj.core.api.Assertions.assertThat;


class ObservabilityAutoConfigurationTest {

    @Test
    void customizerAddsApplicationTag() {
        ObservabilityAutoConfiguration.ObservabilityProperties props = new ObservabilityAutoConfiguration.ObservabilityProperties();
        props.setApplicationName("test-app");
        ObservabilityAutoConfiguration cfg = new ObservabilityAutoConfiguration();
        MeterRegistryCustomizer<MeterRegistry> customizer = cfg.metricsCommonTagsCustomizer(props);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        customizer.customize(registry);
        registry.counter("my.counter").increment();
        assertThat(registry.get("my.counter").counter().getId().getTag("application")).isEqualTo("test-app");
    }
}
