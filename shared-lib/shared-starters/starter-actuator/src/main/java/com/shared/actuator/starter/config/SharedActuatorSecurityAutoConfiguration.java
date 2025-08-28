package com.shared.actuator.starter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
@EnableConfigurationProperties(SharedActuatorProperties.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "shared.actuator.security", name = "enabled", havingValue = "true")
public class SharedActuatorSecurityAutoConfiguration {

  /**
   * Dedicated chain for /actuator/** only.
   * Allows health/ready/live/whoami; prometheus is configurable; the rest requires ROLE_ACTUATOR.
   */
  @Bean(name = "actuatorSecurityFilterChain")
  @Order(1)
  @ConditionalOnMissingBean(name = "actuatorSecurityFilterChain")
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http, SharedActuatorProperties props) throws Exception {
    boolean permitPrometheus = props.getSecurity().isPermitPrometheus();

    http
      .securityMatcher("/actuator/**")
      .authorizeHttpRequests(auth -> {
        auth
          .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
          .requestMatchers("/actuator/ready", "/actuator/live").permitAll()
          .requestMatchers("/actuator/whoami").permitAll();

        if (permitPrometheus) {
          auth.requestMatchers("/actuator/prometheus").permitAll();
        } else {
          auth.requestMatchers("/actuator/prometheus").hasRole("ACTUATOR");
        }

        auth.anyRequest().hasRole("ACTUATOR");
      })
      .httpBasic(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
