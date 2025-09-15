package com.ejada.sec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application-wide password encoder configuration.
 */
@Configuration
public class PasswordEncoderConfig {

  /**
   * Provides a {@link PasswordEncoder} bean using BCrypt hashing.
   *
   * @return password encoder instance
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
