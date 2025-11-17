package com.ejada.email.sending;

import com.ejada.email.sending.config.EmailSendingProperties;
import com.ejada.email.sending.config.KafkaTopicsProperties;
import com.ejada.email.sending.config.RateLimitProperties;
import com.ejada.email.sending.config.SendGridProperties;
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
