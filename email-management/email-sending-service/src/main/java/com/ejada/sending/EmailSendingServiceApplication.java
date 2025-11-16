package com.ejada.sending;

import com.ejada.sending.config.KafkaTopicsProperties;
import com.ejada.sending.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties({KafkaTopicsProperties.class, RateLimitProperties.class})
public class EmailSendingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailSendingServiceApplication.class, args);
  }
}
