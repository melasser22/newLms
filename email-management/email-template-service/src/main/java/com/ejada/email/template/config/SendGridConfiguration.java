package com.ejada.email.template.config;

import com.sendgrid.SendGrid;
import jakarta.annotation.PostConstruct;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendGridConfiguration {

  @PostConstruct
  public void registerSecurityProvider() {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  @Bean
  public SendGrid sendGrid(@Value("${email.sendgrid.api-key:changeme}") String apiKey) {
    return new SendGrid(apiKey);
  }
}
