package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PostgresTestExtension.class)
@Disabled("Requires Docker runtime for Testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class PostgresTestExtensionTest {

    @Test
    void startsContainerAndSetsDatasourceProperties() {
        assertThat(PostgresTestExtension.getContainer().isRunning()).isTrue();
        assertThat(System.getProperty("spring.datasource.url")).isEqualTo(PostgresTestExtension.getContainer().getJdbcUrl());
        assertThat(System.getProperty("spring.datasource.username")).isEqualTo(PostgresTestExtension.getContainer().getUsername());
        assertThat(System.getProperty("spring.datasource.password")).isEqualTo(PostgresTestExtension.getContainer().getPassword());
    }
}
