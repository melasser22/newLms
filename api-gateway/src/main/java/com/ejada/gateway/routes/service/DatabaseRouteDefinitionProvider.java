package com.ejada.gateway.routes.service;

import com.ejada.gateway.config.GatewayRouteDefinitionProvider;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.routes.repository.RouteDefinitionRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@DependsOn("gatewayRouteSchemaInitializer")
public class DatabaseRouteDefinitionProvider implements GatewayRouteDefinitionProvider {

  private final RouteDefinitionRepository repository;
  private final RouteDefinitionConverter converter;

  public DatabaseRouteDefinitionProvider(RouteDefinitionRepository repository,
      RouteDefinitionConverter converter) {
    this.repository = repository;
    this.converter = converter;
  }

  @Override
  public Flux<GatewayRoutesProperties.ServiceRoute> loadRoutes() {
    return repository.findActiveRoutes()
        .flatMapIterable(converter::toServiceRoutes)
        .onErrorResume(ex -> {
          // The database may not be ready when the provider is invoked during startup.
          return Flux.empty();
        });
  }

  @Override
  public String getProviderName() {
    return "DatabaseRouteDefinitionProvider";
  }
}
