package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(KafkaTestExtension.class)
@Disabled("Requires Docker runtime for Testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class KafkaTestExtensionTest {

    @Test
    void startsKafkaAndSetsBootstrapServers() {
        assertThat(KafkaTestExtension.isRunning()).isTrue();
        assertThat(System.getProperty("spring.kafka.bootstrap-servers"))
                .isEqualTo(KafkaTestExtension.getBootstrapServers());
        assertThat(System.getProperty("shared.kafka.bootstrap-servers"))
                .isEqualTo(KafkaTestExtension.getBootstrapServers());
    }
}
