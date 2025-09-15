package com.ejada.headers.starter.config;

import com.ejada.headers.starter.client.FeignHeaderInterceptor;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(prefix = "shared.headers.propagation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SharedHeadersFeignAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(RequestInterceptor.class)
  public RequestInterceptor feignHeaderInterceptor(SharedHeadersProperties props) {
    return new FeignHeaderInterceptor(props);
  }
}
