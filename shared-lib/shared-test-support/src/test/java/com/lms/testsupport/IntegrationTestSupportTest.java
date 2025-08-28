package com.lms.testsupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class IntegrationTestSupportTest extends IntegrationTestSupport {

    @Test
    void containersShouldStart() {
        assertTrue(POSTGRES.isRunning(), "Postgres container should be running");
        assertTrue(REDIS.isRunning(), "Redis container should be running");
    }
}
