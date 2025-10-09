# API Gateway Troubleshooting Guide

## Recent Error Resolution (2025-10-08)

### Issue Description
The API Gateway was experiencing errors with the following symptoms:
- Error log: "Error starting response. Replying error status"
- Reactor Netty channel operations failing
- HTTP connections being terminated immediately after error
- No routes loaded (routes count: 0)
- Database schema initialization failing silently

### Root Causes Identified

1. **No Route Found - Primary Cause**
   - Spring Cloud Gateway had **zero routes loaded**
   - When requests came in, no route matched, causing Spring to throw ResponseStatusException(404)
   - This error occurred in the Netty layer BEFORE reaching custom error handlers
   - Result: "Error starting response" from Reactor Netty

2. **Database Schema Initialization Failures**
   - Route schema SQL statements were failing during execution
   - Errors were being silently swallowed by `onErrorResume(error -> Mono.empty())`
   - No diagnostic logging to identify which SQL statements failed
   - Database routes could not be loaded due to schema issues

3. **ObjectMapper Bean Injection Issue**
   - The `GatewayErrorWebExceptionHandler` was directly injecting `ObjectMapper` with `@Qualifier("jacksonObjectMapper")`
   - When the bean wasn't available or couldn't be resolved, this caused a failure during error response serialization
   - The failure in the error handler itself led to generic Netty errors

4. **Insufficient Error Handling**
   - No specialized handler for 404 (route not found) errors
   - No fallback mechanism when JSON serialization failed
   - Limited diagnostic logging to identify the actual root cause
   - No startup validation of critical beans

### Solutions Implemented

#### 1. Added NOT_FOUND Error Handler
**File**: `api-gateway/src/main/java/com/ejada/gateway/config/GatewayNotFoundHandler.java` (NEW)

Created a dedicated error handler for 404 NOT_FOUND errors when no route matches:
```java
@Component
@Order(-1) // Higher priority than default error handler
public class GatewayNotFoundHandler extends AbstractErrorWebExceptionHandler {
  // Specifically handles ResponseStatusException with 404 status
  // Returns JSON response with correlation ID, tenant ID, and request details
  // Logs warnings for troubleshooting route matching issues
}
```

**Benefits**:
- Provides clear JSON error responses when no route matches
- Logs route matching failures with full context
- Prevents generic Netty "Error starting response" errors
- Helps diagnose routing configuration issues

#### 2. Enhanced Database Schema Initialization
**File**: `api-gateway/src/main/java/com/ejada/gateway/config/RouteSchemaInitializer.java`

Improved logging and error handling in schema initialization:
```java
private Mono<Void> executeStatement(String sql) {
  return databaseClient.sql(sql).fetch().rowsUpdated()
      .doOnNext(count -> LOGGER.debug("Successfully executed schema statement, rows affected: {}", count))
      .doOnError(error -> {
        LOGGER.error("Failed to execute route schema statement", error);
        LOGGER.debug("Failed SQL statement: {}", sql);
      })
      .onErrorResume(error -> {
        LOGGER.debug("Continuing despite schema statement error: {}", error.getMessage());
        return Mono.empty();
      })
      .then();
}
```

**Benefits**:
- Shows which SQL statements succeed/fail
- Logs the actual failing SQL for debugging
- Continues startup even if some statements fail
- Provides visibility into database issues

#### 3. Enhanced ObjectMapper Injection
**File**: `api-gateway/src/main/java/com/ejada/gateway/error/GatewayErrorWebExceptionHandler.java`

Changed from direct injection to `ObjectProvider` pattern:
```java
// Before
public GatewayErrorWebExceptionHandler(
    @Qualifier("jacksonObjectMapper") ObjectMapper jacksonObjectMapper,
    ObjectProvider<ObjectMapper> objectMapperProvider) {
  this.objectMapper = (jacksonObjectMapper != null) ? jacksonObjectMapper
      : objectMapperProvider.getIfAvailable(ObjectMapper::new);
}

// After
public GatewayErrorWebExceptionHandler(
    @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> jacksonObjectMapperProvider,
    ObjectProvider<ObjectMapper> objectMapperProvider) {
  ObjectMapper jacksonObjectMapper = jacksonObjectMapperProvider.getIfAvailable();
  this.objectMapper = (jacksonObjectMapper != null) ? jacksonObjectMapper
      : objectMapperProvider.getIfAvailable(ObjectMapper::new);
  LOGGER.info("GatewayErrorWebExceptionHandler initialized with ObjectMapper: {}",
      this.objectMapper.getClass().getName());
}
```

**Benefits**:
- Graceful fallback to any available ObjectMapper
- Startup logging to confirm successful initialization
- Prevents NPE during error handling

#### 2. Enhanced Error Logging and Fallback Response
**File**: `api-gateway/src/main/java/com/ejada/gateway/error/GatewayErrorWebExceptionHandler.java`

Added comprehensive logging:
```java
// Log the exception details for troubleshooting
if (status.is5xxServerError()) {
  LOGGER.error("Gateway error [correlationId={}, tenantId={}, path={}, status={}]: {}",
      correlationId, tenantId, exchange.getRequest().getPath().value(), status.value(), message, ex);
} else {
  LOGGER.debug("Gateway client error [correlationId={}, path={}, status={}]: {}",
      correlationId, exchange.getRequest().getPath().value(), status.value(), message);
}
```

Added fallback plain text error response:
```java
private Mono<Void> writeFallbackErrorResponse(ServerHttpResponse response, HttpStatus status,
    String errorCode, String message, String correlationId) {
  try {
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    String plainTextError = String.format(
        "Error Code: %s%nStatus: %d%nMessage: %s%nCorrelation ID: %s",
        errorCode, status.value(), message, correlationId != null ? correlationId : "unknown");
    DataBuffer buffer = response.bufferFactory().wrap(plainTextError.getBytes());
    return response.writeWith(Mono.just(buffer));
  } catch (Exception fallbackException) {
    LOGGER.error("Failed to write fallback error response", fallbackException);
    return response.setComplete();
  }
}
```

**Benefits**:
- Detailed error context for troubleshooting
- Correlation ID tracking across requests
- Plain text fallback when JSON serialization fails
- Multiple layers of error handling resilience

#### 3. ObjectMapper Configuration Bean
**File**: `api-gateway/src/main/java/com/ejada/gateway/config/GatewayObjectMapperConfiguration.java`

Created new configuration to ensure the qualified bean exists:
```java
@Configuration
public class GatewayObjectMapperConfiguration {
  @Bean(name = "jacksonObjectMapper")
  @ConditionalOnMissingBean(name = "jacksonObjectMapper")
  public ObjectMapper jacksonObjectMapper(ObjectMapper primaryObjectMapper) {
    LOGGER.info("Registering jacksonObjectMapper qualifier for existing ObjectMapper bean");
    return primaryObjectMapper;
  }
}
```

**Benefits**:
- Ensures the `jacksonObjectMapper` qualifier always resolves
- Reuses the primary ObjectMapper from shared-lib
- Logs when the bean is registered

#### 4. Enhanced Startup Diagnostics
**File**: `api-gateway/src/main/java/com/ejada/gateway/ApiGatewayApplication.java`

Added startup and ready event logging:
```java
@Bean
public ApplicationListener<ApplicationReadyEvent> applicationReadyListener() {
  return event -> {
    LOGGER.info("API Gateway application is ready and accepting requests");
    LOGGER.info("Active profiles: {}", String.join(", ", event.getApplicationContext().getEnvironment().getActiveProfiles()));
    LOGGER.info("Server port: {}", event.getApplicationContext().getEnvironment().getProperty("server.port", "8000"));
  };
}
```

**Benefits**:
- Clear indication of successful startup
- Profile and configuration visibility
- Easier troubleshooting of startup issues

#### 5. Enhanced Health Check Logging
**File**: `api-gateway/src/main/java/com/ejada/gateway/admin/GatewayReadinessIndicator.java`

Added error logging to health checks:
```java
return adminAggregationService.fetchDetailedHealth()
    .map(this::mapToHealth)
    .doOnError(ex -> LOGGER.error("Health check failed: {}", ex.getMessage(), ex))
    .onErrorResume(ex -> Mono.just(Health.down(ex).build()));
```

**Benefits**:
- Visibility into health check failures
- Better monitoring and alerting capabilities

## Troubleshooting Steps

### 1. Check Application Logs
Look for these key log messages at startup:
```
Starting API Gateway application...
GatewayObjectMapperConfiguration: Registering jacksonObjectMapper qualifier...
GatewayNotFoundHandler initialized with ObjectMapper: com.fasterxml.jackson.databind.ObjectMapper
GatewayErrorWebExceptionHandler initialized with ObjectMapper: com.fasterxml.jackson.databind.ObjectMapper
Ensuring gateway route schema is present...
Registered route setup-service -> lb://setup-service...
API Gateway application is ready and accepting requests
Active profiles: dev
Server port: 8000
```

### 2. Verify Routes Are Loaded
Check for route registration messages:
```
Registered route <route-id> -> <uri> ([<paths>])
New routes count: X  (where X > 0)
```

If you see "New routes count: 0", no routes are loaded and ALL requests will return 404.

### 2. Verify ObjectMapper Bean
Check that the ObjectMapper is properly configured:
```bash
curl http://localhost:8000/actuator/beans | jq '.contexts[].beans | to_entries[] | select(.key | contains("objectMapper"))'
```

### 3. Monitor Error Logs
Enable DEBUG logging to see detailed error information:
```yaml
logging:
  level:
    com.ejada.gateway: DEBUG
    com.ejada.gateway.error: DEBUG
```

### 4. Check Health Endpoint
Verify gateway health:
```bash
curl http://localhost:8000/actuator/health
```

### 5. Review Correlation IDs
All errors include correlation IDs for tracking:
```json
{
  "correlationId": "abc-123-def",
  "errorCode": "ERR_INTERNAL",
  "message": "Error details..."
}
```

## Prevention Best Practices

1. **Always use ObjectProvider for optional dependencies**
   ```java
   @Qualifier("beanName") ObjectProvider<BeanType> beanProvider
   ```

2. **Add startup validation logging for critical beans**
   ```java
   LOGGER.info("Component initialized with dependency: {}", dependency.getClass());
   ```

3. **Implement fallback mechanisms in error handlers**
   - Plain text responses when JSON fails
   - Default values for missing configuration
   - Graceful degradation

4. **Use comprehensive error logging**
   - Include correlation IDs
   - Log full exception stack traces for 5xx errors
   - Use appropriate log levels (ERROR, WARN, DEBUG)

5. **Monitor health endpoints regularly**
   - Set up alerts for health check failures
   - Monitor circuit breaker states
   - Track downstream service availability

## Related Configuration

### Application Configuration
Key configuration in `application.yaml`:
```yaml
logging:
  level:
    com.ejada.gateway: DEBUG
    org.springframework.web.server: DEBUG
    reactor.netty: DEBUG

shared:
  core:
    correlation:
      enabled: true
      generate-if-missing: true
```

### Dependencies
The ObjectMapper is provided by:
- `shared-lib/shared-starters/starter-core` → `JacksonConfig.java`
- Configured with JavaTimeModule, ISO-8601 dates, non-null serialization

## Additional Resources

- Spring WebFlux Error Handling: https://docs.spring.io/spring-framework/reference/web/webflux/dispatcher-handler.html
- Reactor Netty Documentation: https://projectreactor.io/docs/netty/release/reference/
- ObjectMapper Configuration: https://github.com/FasterXML/jackson-docs
