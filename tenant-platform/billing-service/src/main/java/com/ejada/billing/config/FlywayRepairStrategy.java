package com.ejada.billing.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customizes Flyway to automatically repair the schema history table before
 * applying migrations. This updates checksums when migration scripts change
 * without requiring manual intervention.
 */
@Configuration
public class FlywayRepairStrategy {

    /**
     * Repairs the Flyway schema history table and then runs migrations.
     *
     * @return strategy that repairs then migrates
     */
    @Bean
    public FlywayMigrationStrategy repairThenMigrateStrategy() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }

    /**
     * Ignores missing historical migrations to allow systems with existing
     * schemas to start even if early migrations were renumbered.
     *
     * @return configuration customizer adding ignore pattern
     */
    @Bean
    public FlywayConfigurationCustomizer ignoreMissingMigrationsCustomizer() {
        return configuration -> configuration.ignoreMigrationPatterns("1:ignored");
    }
}

