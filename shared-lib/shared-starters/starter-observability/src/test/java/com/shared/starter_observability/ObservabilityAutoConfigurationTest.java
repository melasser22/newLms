package com.shared.starter_observability;

import io.micrometer.core.instrument.MeterRegistry;
<<<<<<< HEAD

=======
>>>>>>> cce2a19 (chore: enhance shared library)
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertEquals;
=======
import static org.assertj.core.api.Assertions.assertThat;
>>>>>>> cce2a19 (chore: enhance shared library)

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
