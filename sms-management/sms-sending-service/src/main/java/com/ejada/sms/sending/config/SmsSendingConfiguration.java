package com.ejada.sms.sending.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SmsSendingConfiguration {

  @Bean
  public WebClient smsWebClient(WebClient.Builder builder, CequensProperties properties) {
    return builder.baseUrl(properties.getBaseUrl()).build();
  }
}
