package com.ejada.gateway.transformation;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import com.ejada.gateway.config.GatewayTransformationProperties;
import com.ejada.gateway.metrics.GatewayMetrics;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Wraps downstream responses with the platform {@link BaseResponse} envelope.
 */
public class ResponseBodyTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBodyTransformer.class);

  private final GatewayTransformationProperties properties;

  private final ObjectMapper objectMapper;

  private final GatewayMetrics metrics;

  private final BuildProperties buildProperties;

  public ResponseBodyTransformer(GatewayTransformationProperties properties,
      ObjectMapper objectMapper,
      GatewayMetrics metrics,
      BuildProperties buildProperties) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.buildProperties = buildProperties;
  }

  public boolean isWrapEnabled(MediaType contentType) {
    if (!properties.getResponse().isWrapEnabled()) {
      return false;
    }
    if (contentType == null) {
      return true;
    }
    return MediaType.APPLICATION_JSON.includes(contentType)
        || MediaType.APPLICATION_PROBLEM_JSON.includes(contentType)
        || (contentType.getSubtype() != null && contentType.getSubtype().endsWith("+json"));
  }

  public byte[] wrapBody(ServerWebExchange exchange, byte[] original, HttpStatus status, long startNanos) {
    if (original == null) {
      original = new byte[0];
    }
    Object payload = null;
    try {
      if (original.length > 0) {
        payload = objectMapper.readValue(original, Object.class);
      }
    } catch (Exception ex) {
      LOGGER.debug("Response body is not JSON, skipping wrapping", ex);
      return original;
    }

    try {
      JsonNode jsonNode = (payload != null) ? objectMapper.valueToTree(payload) : null;
      if (jsonNode instanceof ObjectNode objectNode
          && objectNode.hasNonNull("status")
          && objectNode.has("code")
          && objectNode.has("data")) {
        attachMetadata(exchange, objectNode, startNanos);
        metrics.recordResponseTransformation();
        return objectMapper.writeValueAsBytes(objectNode);
      }

      ApiStatus apiStatus = (status != null && status.is2xxSuccessful()) ? ApiStatus.SUCCESS : ApiStatus.ERROR;
      String code = (status != null) ? status.value() + "" : "200";
      String message = (status != null) ? status.getReasonPhrase() : null;

      Map<String, Object> envelope = new LinkedHashMap<>();
      envelope.put("metadata", buildMetadata(exchange, startNanos));
      envelope.put("payload", payload);

      BaseResponse<Map<String, Object>> wrapped = BaseResponse.<Map<String, Object>>builder()
          .status(apiStatus)
          .code(code)
          .message(message)
          .data(envelope)
          .build();
      metrics.recordResponseTransformation();
      return objectMapper.writeValueAsBytes(wrapped);
    } catch (Exception ex) {
      LOGGER.warn("Failed to wrap response body", ex);
      return original;
    }
  }

  private void attachMetadata(ServerWebExchange exchange, ObjectNode node, long startNanos) {
    Map<String, Object> metadata = buildMetadata(exchange, startNanos);
    node.set("metadata", objectMapper.valueToTree(metadata));
  }

  private Map<String, Object> buildMetadata(ServerWebExchange exchange, long startNanos) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    String correlationId = exchange.getAttribute(GatewayRequestAttributes.CORRELATION_ID);
    if (!StringUtils.hasText(correlationId)) {
      correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
    }
    if (!StringUtils.hasText(correlationId)) {
      correlationId = ContextManager.getCorrelationId();
    }
    if (StringUtils.hasText(correlationId)) {
      metadata.put("requestId", correlationId);
    }

    long elapsedMillis = (startNanos > 0) ? Duration.ofNanos(System.nanoTime() - startNanos).toMillis() : 0L;
    metadata.put("processingTime", elapsedMillis);

    String version = properties.getResponse().getVersion();
    if (buildProperties != null && StringUtils.hasText(buildProperties.getVersion())) {
      version = buildProperties.getVersion();
    }
    metadata.put("version", version);
    return metadata;
  }
}

