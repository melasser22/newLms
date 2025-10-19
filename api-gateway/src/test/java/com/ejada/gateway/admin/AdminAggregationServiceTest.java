package com.ejada.gateway.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.AdminServiceState;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import com.ejada.gateway.config.AdminAggregationProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AdminAggregationServiceTest {

  private static <T> ObjectProvider<T> provider(T instance) {
    return new ObjectProvider<>() {
      @Override
      public T getObject(Object... args) {
        return instance;
      }

      @Override
      public T getObject() {
        return instance;
      }

      @Override
      public T getIfAvailable() {
        return instance;
      }

      @Override
      public T getIfUnique() {
        return instance;
      }

      @Override
      public void ifAvailable(java.util.function.Consumer<T> consumer) {
        if (instance != null) {
          consumer.accept(instance);
        }
      }

      @Override
      public void ifUnique(java.util.function.Consumer<T> consumer) {
        if (instance != null) {
          consumer.accept(instance);
        }
      }

      @Override
      public T getIfAvailable(java.util.function.Supplier<T> supplier) {
        return instance != null ? instance : supplier.get();
      }

      @Override
      public T getIfUnique(java.util.function.Supplier<T> supplier) {
        return instance != null ? instance : supplier.get();
      }

      @Override
      public java.util.stream.Stream<T> stream() {
        return instance != null ? java.util.stream.Stream.of(instance) : java.util.stream.Stream.empty();
      }

      @Override
      public java.util.stream.Stream<T> orderedStream() {
        return stream();
      }

      @Override
      public java.util.Iterator<T> iterator() {
        return stream().iterator();
      }
    };
  }

  @Test
  void collectDownstreamSnapshotsEmitsErrorWhenServiceConfigurationInvalid() {
    AdminAggregationProperties properties = new AdminAggregationProperties();
    AdminAggregationProperties.Service invalidService = new AdminAggregationProperties.Service();
    invalidService.setUri(URI.create("http://localhost:9000"));
    properties.getAggregation().setServices(List.of(invalidService));

    WebClient.Builder webClientBuilder = WebClient.builder();
    AdminAggregationService aggregationService = new AdminAggregationService(
        webClientBuilder,
        properties,
        new GatewayRoutesProperties(),
        provider(null),
        provider(null),
        provider(null));

    StepVerifier.create(aggregationService.collectDownstreamSnapshots())
        .expectErrorSatisfies(ex -> assertThat(ex)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("gateway.admin.aggregation.services[0].id"))
        .verify();
  }

  @Test
  void collectDownstreamSnapshotsUsesIndexWhenServiceIdBlank() {
    AdminAggregationProperties properties = new AdminAggregationProperties();
    AdminAggregationProperties.Service invalidService = new AdminAggregationProperties.Service();
    invalidService.setId("  ");
    invalidService.setUri(URI.create("http://localhost:9000"));
    properties.getAggregation().setServices(List.of(invalidService));

    WebClient.Builder webClientBuilder = WebClient.builder();
    AdminAggregationService aggregationService = new AdminAggregationService(
        webClientBuilder,
        properties,
        new GatewayRoutesProperties(),
        provider(null),
        provider(null),
        provider(null));

    StepVerifier.create(aggregationService.collectDownstreamSnapshots())
        .expectErrorSatisfies(ex -> assertThat(ex)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("gateway.admin.aggregation.services[0].id"))
        .verify();
  }

  @Test
  void readinessIndicatorReportsDownWhenAggregationFails() {
    AdminAggregationService aggregationService = mock(AdminAggregationService.class);
    RuntimeException failure = new RuntimeException("aggregation misconfigured");
    when(aggregationService.fetchDetailedHealth()).thenReturn(Mono.error(failure));

    GatewayReadinessIndicator readinessIndicator = new GatewayReadinessIndicator(aggregationService);

    StepVerifier.create(readinessIndicator.health())
        .assertNext(health -> {
          assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
          assertThat(health.getDetails()).containsValue(failure);
        })
        .verifyComplete();

    Mockito.verify(aggregationService).fetchDetailedHealth();
  }

  @Test
  void readinessIndicatorTreatsUnknownRedisStatusAsHealthy() {
    AdminAggregationService aggregationService = mock(AdminAggregationService.class);
    DetailedHealthStatus healthy = new DetailedHealthStatus(
        DetailedHealthStatus.RedisHealthStatus.unavailable(),
        List.of(),
        List.of());
    when(aggregationService.fetchDetailedHealth()).thenReturn(Mono.just(healthy));

    GatewayReadinessIndicator readinessIndicator = new GatewayReadinessIndicator(aggregationService);

    StepVerifier.create(readinessIndicator.health())
        .assertNext(health -> assertThat(health.getStatus().getCode()).isEqualTo("UP"))
        .verifyComplete();

    Mockito.verify(aggregationService).fetchDetailedHealth();
  }

  @Test
  void collectDownstreamSnapshotsCapturesFailuresAsSnapshots() {
    AdminAggregationProperties properties = new AdminAggregationProperties();
    AdminAggregationProperties.Service downstream = new AdminAggregationProperties.Service();
    downstream.setId("billing-service");
    downstream.setUri(URI.create("http://billing-service"));
    downstream.setRequired(false);
    properties.getAggregation().setServices(List.of(downstream));

    WebClient.Builder webClientBuilder = WebClient.builder()
        .exchangeFunction(request -> Mono.error(new IllegalStateException("connection refused")));

    AdminAggregationService aggregationService = new AdminAggregationService(
        webClientBuilder,
        properties,
        new GatewayRoutesProperties(),
        provider(null),
        provider(null),
        provider(null));

    StepVerifier.create(aggregationService.collectDownstreamSnapshots())
        .assertNext(snapshots -> {
          assertThat(snapshots).hasSize(1);
          AdminServiceSnapshot snapshot = snapshots.get(0);
          assertThat(snapshot.serviceId()).isEqualTo("billing-service");
          assertThat(snapshot.required()).isFalse();
          assertThat(snapshot.state()).isEqualTo(AdminServiceState.DOWN);
          assertThat(snapshot.details()).containsEntry("message", "connection refused");
        })
        .verifyComplete();
  }

  @Test
  void collectDownstreamSnapshotsShortCircuitsOptionalServicesWithoutInstances() {
    AdminAggregationProperties properties = new AdminAggregationProperties();
    AdminAggregationProperties.Service optionalService = new AdminAggregationProperties.Service();
    optionalService.setId("policy-service");
    optionalService.setUri(URI.create("lb://policy-service"));
    optionalService.setRequired(false);
    properties.getAggregation().setServices(List.of(optionalService));

    AtomicInteger invocationCount = new AtomicInteger();
    WebClient.Builder webClientBuilder = WebClient.builder()
        .exchangeFunction(request -> {
          invocationCount.incrementAndGet();
          return Mono.error(new AssertionError("WebClient should not be invoked when no instances exist"));
        });

    LoadBalancerHealthCheckAggregator aggregator = new LoadBalancerHealthCheckAggregator();

    AdminAggregationService aggregationService = new AdminAggregationService(
        webClientBuilder,
        properties,
        new GatewayRoutesProperties(),
        provider(null),
        provider(null),
        provider(aggregator));

    StepVerifier.create(aggregationService.collectDownstreamSnapshots())
        .assertNext(snapshots -> {
          assertThat(snapshots).hasSize(1);
          AdminServiceSnapshot snapshot = snapshots.get(0);
          assertThat(snapshot.serviceId()).isEqualTo("policy-service");
          assertThat(snapshot.state()).isEqualTo(AdminServiceState.DOWN);
          assertThat(snapshot.details()).containsEntry("message", "No service instances discovered");
        })
        .verifyComplete();

    assertThat(invocationCount).hasValue(0);
  }

  @Test
  void fetchSnapshotPropagatesContextHeadersWhenMissing() {
    AdminAggregationProperties properties = new AdminAggregationProperties();
    AdminAggregationProperties.Service service = new AdminAggregationProperties.Service();
    service.setId("tenant-service");
    service.setUri(URI.create("http://tenant-service"));
    properties.getAggregation().setServices(List.of(service));

    AtomicReference<ClientRequest> captured = new AtomicReference<>();
    WebClient.Builder webClientBuilder = WebClient.builder()
        .exchangeFunction(request -> {
          captured.set(request);
          return Mono.just(ClientResponse.create(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body("{\"status\":\"UP\"}")
              .build());
        });

    ContextManager.setCorrelationId("corr-123");
    ContextManager.Tenant.set("tenant-xyz");

    try {
      AdminAggregationService aggregationService = new AdminAggregationService(
          webClientBuilder,
          properties,
          new GatewayRoutesProperties(),
          provider(null),
          provider(null),
          provider(null));

      StepVerifier.create(aggregationService.collectDownstreamSnapshots())
          .assertNext(snapshots -> assertThat(snapshots).hasSize(1))
          .verifyComplete();
    } finally {
      ContextManager.clearCorrelationId();
      ContextManager.Tenant.clear();
    }

    ClientRequest request = captured.get();
    assertThat(request).isNotNull();
    assertThat(request.headers().getFirst(HeaderNames.CORRELATION_ID)).isEqualTo("corr-123");
    assertThat(request.headers().getFirst(HeaderNames.X_TENANT_ID)).isEqualTo("tenant-xyz");
  }

  @Test
  void fetchSnapshotDoesNotOverrideExplicitHeaders() {
    AdminAggregationProperties properties = new AdminAggregationProperties();
    AdminAggregationProperties.Service service = new AdminAggregationProperties.Service();
    service.setId("catalog-service");
    service.setUri(URI.create("http://catalog-service"));
    service.setHeaders(Map.of(
        HeaderNames.CORRELATION_ID, "preset-correlation",
        HeaderNames.X_TENANT_ID, "preset-tenant"));
    properties.getAggregation().setServices(List.of(service));

    AtomicReference<ClientRequest> captured = new AtomicReference<>();
    WebClient.Builder webClientBuilder = WebClient.builder()
        .exchangeFunction(request -> {
          captured.set(request);
          return Mono.just(ClientResponse.create(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body("{\"status\":\"UP\"}")
              .build());
        });

    ContextManager.setCorrelationId("corr-override");
    ContextManager.Tenant.set("tenant-override");

    try {
      AdminAggregationService aggregationService = new AdminAggregationService(
          webClientBuilder,
          properties,
          new GatewayRoutesProperties(),
          provider(null),
          provider(null),
          provider(null));

      StepVerifier.create(aggregationService.collectDownstreamSnapshots())
          .assertNext(snapshots -> assertThat(snapshots).hasSize(1))
          .verifyComplete();
    } finally {
      ContextManager.clearCorrelationId();
      ContextManager.Tenant.clear();
    }

    ClientRequest request = captured.get();
    assertThat(request).isNotNull();
    assertThat(request.headers().getFirst(HeaderNames.CORRELATION_ID)).isEqualTo("preset-correlation");
    assertThat(request.headers().getFirst(HeaderNames.X_TENANT_ID)).isEqualTo("preset-tenant");
  }
}

