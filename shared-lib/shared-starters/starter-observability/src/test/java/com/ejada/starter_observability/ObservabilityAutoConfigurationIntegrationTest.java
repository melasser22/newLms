package com.ejada.starter_observability;

import com.ejada.testsupport.IntegrationTestSupport;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ObservabilityAutoConfiguration.class)
class ObservabilityAutoConfigurationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    OpenTelemetry openTelemetry;

    @Autowired
    Tracer tracer;

    @Test
    void providesOpenTelemetryBeans() {
        assertThat(openTelemetry).isNotNull();
        assertThat(tracer).isNotNull();
    }
}
