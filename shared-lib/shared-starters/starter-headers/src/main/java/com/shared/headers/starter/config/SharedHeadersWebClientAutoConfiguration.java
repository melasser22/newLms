package com.shared.headers.starter.config;

import com.shared.headers.starter.client.WebClientHeaderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
@ConditionalOnProperty(prefix = "shared.headers.propagation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SharedHeadersWebClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(WebClientHeaderCustomizer.class)
  public WebClientHeaderCustomizer webClientHeaderCustomizer(SharedHeadersProperties props) {
    return new WebClientHeaderCustomizer(props);
  }
}
