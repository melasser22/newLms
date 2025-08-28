package com.shared.starter_data.time;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
@AutoConfiguration
public class ClockConfig {
  @Bean public Clock clock(){ return Clock.systemUTC(); }
}
