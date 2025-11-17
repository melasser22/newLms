package com.ejada.email.management.config;

import com.ejada.email.management.service.TenantContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  RestClient restClient() {
    return RestClient
        .builder()
        .requestInterceptor(
            (request, body, execution) -> {
              TenantContextHolder
                  .getTenantId()
                  .ifPresent(tenantId -> request.getHeaders().add("X-Tenant-Id", tenantId));
              return execution.execute(request, body);
            })
        .build();
  }
}
