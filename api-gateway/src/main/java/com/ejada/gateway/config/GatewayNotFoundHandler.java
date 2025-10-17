package com.ejada.gateway.config;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Handles 404 NOT_FOUND errors when no route matches the request.
 * This is specifically designed to handle Spring Cloud Gateway's route matching failures.
 */
@Component
@Order(-3) // Highest priority so NOT_FOUND errors short-circuit before generic handlers
public class GatewayNotFoundHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayNotFoundHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayNotFoundHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer,
            @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> jacksonObjectMapperProvider,
            ObjectProvider<ObjectMapper> fallbackObjectMapperProvider) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        super.setMessageWriters(serverCodecConfigurer.getWriters());

        ObjectMapper jacksonObjectMapper = jacksonObjectMapperProvider.getIfAvailable();
        this.objectMapper = (jacksonObjectMapper != null) ? jacksonObjectMapper
                : fallbackObjectMapperProvider.getIfAvailable(ObjectMapper::new);
        LOGGER.info("GatewayNotFoundHandler initialized with ObjectMapper: {}",
                this.objectMapper.getClass().getName());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);

        if (!isNotFoundError(error)) {
            return Mono.error(error);
        }

        String correlationId = resolveCorrelationId(request);
        String tenantId = resolveTenantId(request);
        String path = request.path();
        String method = request.method().name();

        String message = resolveMessage(error, method, path);

        LOGGER.warn("No route found [correlationId={}, tenantId={}, method={}, path={}]",
                correlationId, tenantId, method, path);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", Instant.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        errorResponse.put("method", method);
        errorResponse.put("correlationId", correlationId);
        errorResponse.put("tenantId", tenantId);

        return ServerResponse
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorResponse))
                .doOnError(ex -> LOGGER.error("Failed to render NOT_FOUND response [correlationId={}]: {}",
                        correlationId, ex.getMessage(), ex));
    }

    private boolean isNotFoundError(Throwable error) {
        if (error instanceof ResponseStatusException rse) {
            return rse.getStatusCode().value() == HttpStatus.NOT_FOUND.value();
        }
        return error instanceof NotFoundException;
    }

    private String resolveMessage(Throwable error, String method, String path) {
        if (error instanceof ResponseStatusException rse) {
            String reason = rse.getReason();
            if (StringUtils.hasText(reason)) {
                return reason;
            }
        }
        if (error instanceof NotFoundException gatewayNotFound) {
            String message = gatewayNotFound.getMessage();
            if (StringUtils.hasText(message)) {
                return message;
            }
        }
        return "No route found for " + method + " " + path;
    }

    private String resolveCorrelationId(ServerRequest request) {
        return request.headers().firstHeader(HeaderNames.CORRELATION_ID) != null
                ? request.headers().firstHeader(HeaderNames.CORRELATION_ID)
                : ContextManager.getCorrelationId();
    }

    private String resolveTenantId(ServerRequest request) {
        return request.headers().firstHeader(HeaderNames.X_TENANT_ID) != null
                ? request.headers().firstHeader(HeaderNames.X_TENANT_ID)
                : ContextManager.Tenant.get();
    }
}
