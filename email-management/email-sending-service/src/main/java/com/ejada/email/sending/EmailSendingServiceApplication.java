package com.ejada.sending;

import com.ejada.sending.config.EmailSendingProperties;
import com.ejada.sending.config.KafkaTopicsProperties;
import com.ejada.sending.config.RateLimitProperties;
import com.ejada.sending.config.SendGridProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties({
    KafkaTopicsProperties.class,
    RateLimitProperties.class,
    SendGridProperties.class,
    EmailSendingProperties.class
})
public class EmailSendingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailSendingServiceApplication.class, args);
  }
}
