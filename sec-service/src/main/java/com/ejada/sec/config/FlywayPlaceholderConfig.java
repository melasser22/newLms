package com.ejada.sec.config;

import com.ejada.crypto.password.PasswordHasher;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Flyway placeholders that should be shared across migrations. The
 * default superadmin password is hashed using the shared crypto
 * {@link PasswordHasher} so the application never needs to embed a raw BCrypt
 * string.
 */
@Configuration
public class FlywayPlaceholderConfig {

  private final String superadminPasswordHash;

  public FlywayPlaceholderConfig(
      @Value("${sec.superadmin.bootstrap.default-password:Admin@123!}")
          String defaultSuperadminPassword) {
    this.superadminPasswordHash = PasswordHasher.bcrypt(defaultSuperadminPassword);
  }

  @Bean
  FlywayConfigurationCustomizer sharedPasswordPlaceholdersCustomizer() {
    return this::applySharedPlaceholders;
  }

  private void applySharedPlaceholders(FluentConfiguration configuration) {
    configuration.placeholders(Map.of("superadminPasswordHash", superadminPasswordHash));
  }
}

