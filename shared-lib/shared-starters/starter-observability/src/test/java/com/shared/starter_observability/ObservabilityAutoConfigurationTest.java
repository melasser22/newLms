package com.shared.starter_observability;

import io.micrometer.core.instrument.MeterRegistry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilityAutoConfigurationTest {

    @Test
    void addsApplicationTag() {
        ObservabilityAutoConfiguration config = new ObservabilityAutoConfiguration();
        ObservabilityAutoConfiguration.ObservabilityProps props = new ObservabilityAutoConfiguration.ObservabilityProps();
        props.setApplication("test-app");
        MeterRegistryCustomizer<MeterRegistry> customizer = config.metricsCommonTagsCustomizer(props);
        MeterRegistry registry = new SimpleMeterRegistry();
        customizer.customize(registry);
        registry.counter("demo.counter").increment();
        assertEquals("test-app", registry.find("demo.counter").counter().getId().getTag("application"));
    }
}
