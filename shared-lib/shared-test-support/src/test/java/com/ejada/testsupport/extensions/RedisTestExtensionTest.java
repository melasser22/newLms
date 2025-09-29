package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(RedisTestExtension.class)
@Testcontainers(disabledWithoutDocker = true)
class RedisTestExtensionTest {

    @Test
    void startsContainerAndSetsRedisProperties() {
        assertThat(RedisTestExtension.getContainer().isRunning()).isTrue();
        assertThat(System.getProperty("spring.data.redis.host")).isEqualTo(RedisTestExtension.getContainer().getHost());
        assertThat(System.getProperty("spring.data.redis.port")).isEqualTo(String.valueOf(RedisTestExtension.getContainer().getMappedPort(6379)));
    }
}
