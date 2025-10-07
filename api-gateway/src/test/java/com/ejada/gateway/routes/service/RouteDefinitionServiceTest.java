package com.ejada.gateway.routes.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.gateway.routes.model.RouteComponent;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteDefinitionRequest;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.repository.RouteDefinitionRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RouteDefinitionServiceTest {

  @Mock
  private RouteDefinitionRepository repository;

  @Mock
  private RouteDefinitionValidator validator;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private RouteDefinitionService service;

  @BeforeEach
  void setUp() {
    service = new RouteDefinitionService(repository, validator, eventPublisher);
    when(validator.validate(any(RouteDefinition.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createPublishesRefreshEvent() {
    RouteDefinitionRequest request = new RouteDefinitionRequest(
        "/api/demo/**",
        "http://demo",
        List.of(new RouteComponent("Path", Map.of("pattern", "/api/demo/**"))),
        List.of(),
        RouteMetadata.empty(),
        true);

    RouteDefinition created = buildRoute(UUID.randomUUID(), "/api/demo/**", "http://demo", 1);

    when(repository.create(any(RouteDefinition.class), eq("system")))
        .thenReturn(Mono.just(created));

    StepVerifier.create(service.create(request, null))
        .expectNext(created)
        .verifyComplete();

    verify(repository).create(any(RouteDefinition.class), eq("system"));
    verify(eventPublisher).publishEvent(any());
  }

  @Test
  void updatePublishesRefreshEvent() {
    UUID id = UUID.randomUUID();
    RouteDefinition existing = buildRoute(id, "/api/demo/**", "http://demo", 1);
    RouteDefinition updated = buildRoute(id, "/api/demo/**", "http://demo", 2);

    RouteDefinitionRequest request = new RouteDefinitionRequest(
        "/api/demo/**",
        "http://demo",
        List.of(new RouteComponent("Path", Map.of("pattern", "/api/demo/**"))),
        List.of(),
        RouteMetadata.empty(),
        true);

    when(repository.findById(id)).thenReturn(Mono.just(existing));
    when(repository.update(any(RouteDefinition.class), eq("system")))
        .thenReturn(Mono.just(updated));

    StepVerifier.create(service.update(id, request, null))
        .expectNext(updated)
        .verifyComplete();

    verify(repository).update(any(RouteDefinition.class), eq("system"));
    verify(eventPublisher).publishEvent(any());
  }

  @Test
  void disablePublishesRefreshEvent() {
    UUID id = UUID.randomUUID();
    RouteDefinition disabled = buildRoute(id, "/api/demo/**", "http://demo", 2);

    when(repository.disable(eq(id), eq("officer")))
        .thenReturn(Mono.just(disabled));

    StepVerifier.create(service.disable(id, new TestingAuthenticationToken("officer", "")))
        .verifyComplete();

    verify(repository).disable(eq(id), eq("officer"));
    verify(eventPublisher).publishEvent(any());
  }

  private RouteDefinition buildRoute(UUID id, String path, String uri, int version) {
    Instant now = Instant.now();
    return new RouteDefinition(
        id,
        path,
        URI.create(uri),
        List.of(new RouteComponent("Path", Map.of("pattern", path))),
        List.of(),
        RouteMetadata.empty(),
        true,
        version,
        now,
        now);
  }
}
