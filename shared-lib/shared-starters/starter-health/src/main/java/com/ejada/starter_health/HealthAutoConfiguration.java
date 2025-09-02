package com.ejada.starter_health;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Auto-configuration for the generic health controller.
 */
@AutoConfiguration
public class HealthAutoConfiguration {

    @Bean
    public HealthController healthController(ObjectProvider<DataSource> dataSource,
                                             ObjectProvider<RedisTemplate<String, Object>> redisTemplate,
                                             Environment environment) {
        return new HealthController(
                Optional.ofNullable(dataSource.getIfAvailable()),
                Optional.ofNullable(redisTemplate.getIfAvailable()),
                environment);
    }
}
