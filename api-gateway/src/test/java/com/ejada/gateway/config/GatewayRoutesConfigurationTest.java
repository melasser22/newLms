package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GatewayRoutesConfigurationTest {

  private AnnotationConfigApplicationContext applicationContext;
  private RouteLocatorBuilder routeLocatorBuilder;

  @BeforeEach
  void setUp() {
    applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.registerBean(PathRoutePredicateFactory.class, () -> new PathRoutePredicateFactory());
    applicationContext.registerBean(StripPrefixGatewayFilterFactory.class, StripPrefixGatewayFilterFactory::new);
    applicationContext.refresh();
    routeLocatorBuilder = new RouteLocatorBuilder(applicationContext);
  }

  @AfterEach
  void tearDown() {
    if (applicationContext != null) {
      applicationContext.close();
    }
  }

  @Test
  void gatewayRoutesFailsFastWhenNoRoutesConfigured() {
    GatewayRoutesConfiguration configuration = new GatewayRoutesConfiguration();
    GatewayRoutesProperties properties = new GatewayRoutesProperties();

    assertThatThrownBy(() -> configuration.gatewayRoutes(routeLocatorBuilder,
        properties,
        emptyProvider(),
        emptyProvider(),
        emptyProvider(),
        emptyProvider(),
        emptyProvider(),
        new TenantCircuitBreakerMetrics(new SimpleMeterRegistry())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No gateway routes were configured");
  }

  @Test
  void gatewayRoutesRegistersConfiguredRoute() {
    GatewayRoutesConfiguration configuration = new GatewayRoutesConfiguration();
    GatewayRoutesProperties properties = new GatewayRoutesProperties();
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setId("test-service");
    route.setUri("lb://test-service");
    route.setPaths(List.of("/api/test/**"));
    properties.getRoutes().put("test", route);

    RouteLocator locator = configuration.gatewayRoutes(routeLocatorBuilder,
        properties,
        emptyProvider(),
        emptyProvider(),
        emptyProvider(),
        emptyProvider(),
        emptyProvider(),
        new TenantCircuitBreakerMetrics(new SimpleMeterRegistry()));

    assertThat(locator.getRoutes().collectList().block())
        .anySatisfy(definition -> assertThat(definition.getId()).isEqualTo("test-service"));
  }

  private static <T> ObjectProvider<T> emptyProvider() {
    return new ObjectProvider<>() {

      @Override
      public T getObject(Object... args) throws BeansException {
        throw new NoSuchBeanDefinitionException("No bean available");
      }

      @Override
      public T getIfAvailable() throws BeansException {
        return null;
      }

      @Override
      public T getIfUnique() throws BeansException {
        return null;
      }

      @Override
      public T getIfAvailable(java.util.function.Supplier<T> supplier) throws BeansException {
        return null;
      }

      @Override
      public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
        // no-op
      }

      @Override
      public T getIfUnique(java.util.function.Supplier<T> supplier) throws BeansException {
        return null;
      }

      @Override
      public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
        // no-op
      }

      @Override
      public Stream<T> stream() {
        return Stream.empty();
      }

      @Override
      public Stream<T> orderedStream() {
        return Stream.empty();
      }

      @Override
      public Iterator<T> iterator() {
        return Stream.<T>empty().iterator();
      }
    };
  }
}
