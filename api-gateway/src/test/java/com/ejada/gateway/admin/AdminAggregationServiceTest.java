package com.ejada.gateway.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.AdminServiceState;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import com.ejada.gateway.config.AdminAggregationProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;
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
}

