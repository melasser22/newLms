package com.ejada.gateway.routes.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
  private final RouteVariantService variantService;

  public DatabaseRouteDefinitionProvider(RouteDefinitionRepository repository,
      RouteDefinitionConverter converter,
      RouteVariantService variantService) {
    this.repository = repository;
    this.converter = converter;
    this.variantService = variantService;
  }

  @Override
  public Flux<GatewayRoutesProperties.ServiceRoute> loadRoutes() {
    return repository.findActiveRoutes()
        .collectList()
        .flatMapMany(definitions -> {
          Map<UUID, List<GatewayRoutesProperties.ServiceRoute>> runtime = new LinkedHashMap<>();
          List<GatewayRoutesProperties.ServiceRoute> aggregated = new ArrayList<>();
          for (var definition : definitions) {
            List<GatewayRoutesProperties.ServiceRoute> serviceRoutes = converter.toServiceRoutes(definition);
            runtime.put(definition.id(), serviceRoutes);
            aggregated.addAll(serviceRoutes);
          }
          variantService.rebuild(definitions, runtime);
          return Flux.fromIterable(aggregated);
        });
  }

  @Override
  public String getProviderName() {
    return "DatabaseRouteDefinitionProvider";
  }
}
