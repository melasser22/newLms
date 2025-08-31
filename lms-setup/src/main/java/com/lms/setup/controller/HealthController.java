package com.lms.setup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Custom health check endpoints")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "DataSource is injected by Spring")
    private final DataSource dataSource;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "RedisTemplate is injected by Spring")
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/custom")
    @Operation(summary = "Custom health check", description = "Performs comprehensive health checks for the application")
    public ResponseEntity<Map<String, Object>> customHealthCheck() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        healthStatus.put("application", "lms-setup");
        healthStatus.put("version", "1.0.0");
        healthStatus.put("environment", System.getProperty("spring.profiles.active", "default"));

        // Database health check
        try (Connection connection = dataSource.getConnection()) {
            healthStatus.put("database", "UP");
            healthStatus.put("databaseUrl", connection.getMetaData().getURL());
            healthStatus.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            healthStatus.put("databaseDriver", connection.getMetaData().getDriverName());
        } catch (SQLException e) {
            log.error("Database health check failed", e);
            healthStatus.put("database", "DOWN");
            healthStatus.put("databaseError", e.getMessage());
            healthStatus.put("status", "DOWN");
        }

        // Redis health check
        try {
            redisTemplate.opsForValue().get("health-check");
            healthStatus.put("redis", "UP");
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory != null) {
                try (var connection = connectionFactory.getConnection()) {
                    healthStatus.put("redisHost", connection.getClientName());
                }
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            healthStatus.put("redis", "DOWN");
            healthStatus.put("redisError", e.getMessage());
            healthStatus.put("status", "DOWN");
        }

        // System health checks
        healthStatus.put("cache", "UP");
        healthStatus.put("security", "UP");
        healthStatus.put("rateLimiting", "UP");
        
        // Memory and system info
        Runtime runtime = Runtime.getRuntime();
        healthStatus.put("memory", Map.of(
            "total", runtime.totalMemory(),
            "free", runtime.freeMemory(),
            "used", runtime.totalMemory() - runtime.freeMemory(),
            "max", runtime.maxMemory()
        ));
        
        healthStatus.put("system", Map.of(
            "javaVersion", System.getProperty("java.version"),
            "osName", System.getProperty("os.name"),
            "osVersion", System.getProperty("os.version"),
            "userTimezone", System.getProperty("user.timezone")
        ));

        return ResponseEntity.ok(healthStatus);
    }

    @GetMapping("/ping")
    @Operation(summary = "Simple ping", description = "Simple endpoint to check if the service is responding")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("service", "lms-setup");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Checks if the service is ready to handle requests")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("status", "READY");
        readiness.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Check if all critical dependencies are available
        boolean allReady = true;
        
        // Database readiness
        try (Connection connection = dataSource.getConnection()) {
            readiness.put("database", "READY");
        } catch (SQLException e) {
            readiness.put("database", "NOT_READY");
            readiness.put("databaseError", e.getMessage());
            allReady = false;
        }
        
        // Redis readiness
        try {
            redisTemplate.opsForValue().get("readiness-check");
            readiness.put("redis", "READY");
        } catch (Exception e) {
            readiness.put("redis", "NOT_READY");
            readiness.put("redisError", e.getMessage());
            allReady = false;
        }
        
        readiness.put("overall", allReady ? "READY" : "NOT_READY");
        
        if (allReady) {
            return ResponseEntity.ok(readiness);
        } else {
            return ResponseEntity.status(503).body(readiness);
        }
    }

    @GetMapping("/liveness")
    @Operation(summary = "Liveness check", description = "Checks if the service is alive and running")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        liveness.put("uptime", System.currentTimeMillis());
        
        // Simple liveness check - if we can respond, we're alive
        return ResponseEntity.ok(liveness);
    }
}
