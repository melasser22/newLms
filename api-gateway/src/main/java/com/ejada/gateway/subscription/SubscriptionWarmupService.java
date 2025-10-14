package com.ejada.gateway.subscription;

import com.ejada.gateway.config.SubscriptionValidationProperties;
import com.ejada.gateway.config.SubscriptionWarmupProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Periodically warms the subscription cache by pre-fetching active tenants.
 */
@Component
public class SubscriptionWarmupService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionWarmupService.class);

  private final SubscriptionCacheService cacheService;
  private final SubscriptionWarmupProperties warmupProperties;
  private final SubscriptionValidationProperties validationProperties;
  private final WebClient tenantClient;
  private final AtomicBoolean warmupInProgress = new AtomicBoolean(false);

  public SubscriptionWarmupService(SubscriptionCacheService cacheService,
      SubscriptionWarmupProperties warmupProperties,
      SubscriptionValidationProperties validationProperties,
      WebClient.Builder webClientBuilder) {
    this.cacheService = Objects.requireNonNull(cacheService, "cacheService");
    this.warmupProperties = Objects.requireNonNull(warmupProperties, "warmupProperties");
    this.validationProperties = Objects.requireNonNull(validationProperties, "validationProperties");
    this.tenantClient = webClientBuilder.clone()
        .baseUrl(this.warmupProperties.getTenantServiceUri())
        .build();
  }

  @org.springframework.context.event.EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    triggerWarmup("startup");
  }

  @Scheduled(fixedDelayString = "${gateway.subscription.warmup.refresh-interval:PT2M}")
  public void scheduledWarmup() {
    triggerWarmup("scheduled");
  }

  private void triggerWarmup(String source) {
    if (!warmupProperties.isEnabled() || !validationProperties.isEnabled()) {
      return;
    }
    if (!warmupInProgress.compareAndSet(false, true)) {
      LOGGER.debug("Subscription warmup skipped because a previous run is still active");
      return;
    }
    fetchActiveTenantCodes()
        .flatMapMany(Flux::fromIterable)
        .flatMap(tenant -> cacheService.fetchAndCache(tenant)
            .timeout(warmupTimeout())
            .doOnSuccess(record -> LOGGER.debug("Warmed subscription cache for tenant {} ({})", tenant, source))
            .onErrorResume(ex -> {
              LOGGER.warn("Subscription warmup failed for tenant {}", tenant, ex);
              return Mono.empty();
            })
            .then(), 4)
        .doOnError(ex -> LOGGER.warn("Subscription warmup run failed", ex))
        .doFinally(signal -> warmupInProgress.set(false))
        .subscribe();
  }

  private Duration warmupTimeout() {
    Duration validationTtl = validationProperties.getCacheTtl();
    if (validationTtl == null || validationTtl.isNegative() || validationTtl.isZero()) {
      return Duration.ofSeconds(10);
    }
    return validationTtl;
  }

  private Mono<List<String>> fetchActiveTenantCodes() {
    return fetchPage(0)
        .flatMapMany(first -> {
          int totalPages = Math.max(first.totalPages(), 1);
          List<Mono<PageSlice>> publishers = new ArrayList<>();
          publishers.add(Mono.just(first));
          for (int i = 1; i < totalPages; i++) {
            final int page = i;
            publishers.add(fetchPage(page));
          }
          return Flux.concat(publishers);
        })
        .map(PageSlice::tenants)
        .flatMapIterable(list -> list)
        .filter(StringUtils::hasText)
        .distinct()
        .collectList();
  }

  private Mono<PageSlice> fetchPage(int page) {
    return tenantClient.get()
        .uri(builder -> builder
            .queryParam("active", true)
            .queryParam("size", warmupProperties.getPageSize())
            .queryParam("page", page)
            .build())
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(this::mapPage)
        .doOnError(ex -> LOGGER.warn("Failed to fetch active tenant page {}", page, ex))
        .onErrorReturn(PageSlice.empty());
  }

  private PageSlice mapPage(JsonNode root) {
    if (root == null) {
      return PageSlice.empty();
    }
    JsonNode data = root.path("data");
    if (!data.isObject()) {
      return PageSlice.empty();
    }
    int totalPages = Math.max(data.path("totalPages").asInt(1), 1);
    JsonNode content = data.path("content");
    if (content == null || !content.isArray() || content.isEmpty()) {
      return new PageSlice(Collections.emptyList(), totalPages);
    }
    List<String> tenants = new ArrayList<>();
    content.forEach(node -> {
      if (node == null || !node.isObject()) {
        return;
      }
      boolean active = node.path("active").asBoolean(true);
      if (!active) {
        return;
      }
      String code = textValue(node, "code");
      if (StringUtils.hasText(code)) {
        tenants.add(code.trim());
      }
    });
    return new PageSlice(tenants, totalPages);
  }

  private String textValue(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value == null || value.isMissingNode() || !value.isTextual()) {
      return null;
    }
    return value.asText();
  }

  private record PageSlice(List<String> tenants, int totalPages) {
    PageSlice {
      tenants = CollectionUtils.isEmpty(tenants) ? List.of() : List.copyOf(tenants);
      totalPages = Math.max(totalPages, 1);
    }

    static PageSlice empty() {
      return new PageSlice(List.of(), 1);
    }
  }
}

