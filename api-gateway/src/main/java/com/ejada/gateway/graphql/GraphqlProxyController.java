package com.ejada.gateway.graphql;

import com.ejada.gateway.config.GatewayGraphqlProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** GraphQL proxy endpoint that performs lightweight query analysis before forwarding downstream. */
@RestController
@ConditionalOnProperty(prefix = "gateway.graphql", name = "enabled", havingValue = "true")
public class GraphqlProxyController {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlProxyController.class);

  private final GatewayGraphqlProperties properties;
  private final GraphqlQueryAnalyzer analyzer;
  private final WebClient webClient;

  public GraphqlProxyController(GatewayGraphqlProperties properties,
      GraphqlQueryAnalyzer analyzer,
      WebClient.Builder webClientBuilder) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    if (properties.getUpstreamUri() == null) {
      throw new IllegalStateException("gateway.graphql.upstream-uri must be configured when GraphQL is enabled");
    }
    this.webClient = webClientBuilder.clone().build();
  }

  @PostMapping(path = "/graphql", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<JsonNode>> proxy(@RequestBody GraphqlRequest request) {
    if (request == null || !StringUtils.hasText(request.query())) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "GraphQL query must be provided");
    }

    analyzer.assertWithinLimits(request.query(), properties);

    return webClient.post()
        .uri(properties.getUpstreamUri())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .toEntity(JsonNode.class)
        .timeout(properties.getTimeout())
        .doOnError(ex -> LOGGER.warn("GraphQL proxy failed: {}", ex.toString()));
  }

  public record GraphqlRequest(String query, String operationName, Map<String, Object> variables) {
  }
}

