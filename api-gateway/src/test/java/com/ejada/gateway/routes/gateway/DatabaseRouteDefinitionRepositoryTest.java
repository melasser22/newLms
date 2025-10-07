package com.ejada.gateway.routes.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.gateway.routes.model.RouteComponent;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DatabaseRouteDefinitionRepositoryTest {

  @Mock
  private com.ejada.gateway.routes.repository.RouteDefinitionRepository routeRepository;

  private DatabaseRouteDefinitionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DatabaseRouteDefinitionRepository(routeRepository);
  }

  @Test
  void mapsDomainRouteToGatewayDefinition() {
    RouteDefinition route = buildRoute(UUID.randomUUID(), "lb://demo", Map.of("pattern", "/demo/**"));
    RouteMetadata metadata = route.metadata();
    metadata.setStripPrefix(1);
    metadata.setRequestHeaders(Map.of("X-Test", "true"));

    when(routeRepository.findActiveRoutes()).thenReturn(Flux.just(route));

    StepVerifier.create(repository.getRouteDefinitions())
        .assertNext(definition -> {
          assertThat(definition.getId()).isEqualTo(route.id().toString());
          assertThat(definition.getUri()).isEqualTo(route.serviceUri());
          assertThat(definition.getPredicates()).hasSize(1);
          assertThat(definition.getPredicates().get(0).getName()).isEqualTo("Path");
          assertThat(definition.getPredicates().get(0).getArgs())
              .containsEntry("pattern", "/demo/**");
          assertThat(definition.getMetadata()).containsEntry("stripPrefix", 1);
          assertThat(definition.getMetadata())
              .hasEntrySatisfying("requestHeaders", value -> assertThat(value).isInstanceOf(Map.class));
        })
        .verifyComplete();
  }

  @Test
  void refreshEventClearsCache() {
    RouteDefinition first = buildRoute(UUID.randomUUID(), "lb://demo", Map.of("pattern", "/first/**"));
    RouteDefinition second = buildRoute(UUID.randomUUID(), "lb://demo", Map.of("pattern", "/second/**"));

    when(routeRepository.findActiveRoutes())
        .thenReturn(Flux.just(first))
        .thenReturn(Flux.just(second));

    StepVerifier.create(repository.getRouteDefinitions())
        .expectNextMatches(def -> def.getId().equals(first.id().toString()))
        .verifyComplete();

    StepVerifier.create(repository.getRouteDefinitions())
        .expectNextMatches(def -> def.getId().equals(first.id().toString()))
        .verifyComplete();

    verify(routeRepository, times(1)).findActiveRoutes();

    repository.handleRefreshEvent(new RefreshRoutesEvent(this));

    StepVerifier.create(repository.getRouteDefinitions())
        .expectNextMatches(def -> def.getId().equals(second.id().toString()))
        .verifyComplete();

    verify(routeRepository, times(2)).findActiveRoutes();
  }

  private RouteDefinition buildRoute(UUID id, String uri, Map<String, String> predicateArgs) {
    Instant now = Instant.now();
    RouteMetadata metadata = RouteMetadata.empty();
    return new RouteDefinition(
        id,
        "/demo/**",
        URI.create(uri),
        List.of(new RouteComponent("Path", predicateArgs)),
        List.of(),
        metadata,
        true,
        1,
        now,
        now);
  }
}
