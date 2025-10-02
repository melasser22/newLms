package com.ejada.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

/**
 * Publishes {@link RefreshRoutesEvent} whenever the gateway configuration changes. This allows
 * operators to trigger zero-downtime route reloads after Spring Cloud Config / Kubernetes reload
 * events without bouncing the pod.
 */
@Component
public class GatewayRoutesRefresher {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRoutesRefresher.class);

  private final ApplicationEventPublisher publisher;
  private final ObjectProvider<GatewayRouteDefinitionProvider> providers;

  public GatewayRoutesRefresher(ApplicationEventPublisher publisher,
      ObjectProvider<GatewayRouteDefinitionProvider> providers) {
    this.publisher = publisher;
    this.providers = providers;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    refreshRoutes("application-startup");
  }

  @EventListener({org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent.class,
      org.springframework.cloud.context.environment.EnvironmentChangeEvent.class})
  public void onConfigChange(Object event) {
    refreshRoutes(event.getClass().getSimpleName());
  }

  public void refreshRoutes(String reason) {
    LOGGER.info("Publishing RefreshRoutesEvent due to {}", reason);
    publisher.publishEvent(new RefreshRoutesEvent(this));
    providers.orderedStream().forEach(provider -> {
      provider.loadRoutes()
          .collectList()
          .onErrorResume(ex -> {
            LOGGER.warn("Dynamic route provider {} failed during refresh", provider.getProviderName(), ex);
            return Mono.empty();
          })
          .subscribe(routes -> {
            if (!CollectionUtils.isEmpty(routes)) {
              LOGGER.debug("{} provided {} routes during refresh", provider.getProviderName(), routes.size());
            }
          });
    });
  }
}
