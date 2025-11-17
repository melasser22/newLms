package com.ejada.email.usage.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

  @Bean
  public Clock systemClock() {
    return Clock.systemUTC();
  }
}
