package com.ejada.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=" + ApiGatewayApplicationTests.SECRET,
    "shared.security.resource-server.enabled=false",
    "gateway.routes.test.id=test-route",
    "gateway.routes.test.uri=http://example.org",
    "gateway.routes.test.paths[0]=/test/**",
    "gateway.cache.kafka.enabled=false",
    "shared.ratelimit.enabled=false",
    "spring.autoconfigure.exclude=com.ejada.shared_starter_ratelimit.RateLimitAutoConfiguration",
    "spring.kafka.bootstrap-servers=localhost:65535",
    "spring.kafka.listener.auto-startup=false",
    "spring.application.admin.enabled=false"
})
class ApiGatewayApplicationTests {

  static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";

  @Autowired
  private RouteLocator routeLocator;

  @Test
  void contextLoads() {
    assertThat(routeLocator).isNotNull();
    assertThat(routeLocator.getRoutes().collectList().block()).isNotEmpty();
  }
}
