package com.ejada.starter_health;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic health endpoints available to all Ejada services.
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final Optional<DataSource> dataSource;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final Environment environment;

    @GetMapping("/custom")
    public ResponseEntity<Map<String, Object>> customHealthCheck() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        healthStatus.put("application", environment.getProperty("spring.application.name", "application"));
        healthStatus.put("environment", String.join(",", environment.getActiveProfiles()));

        if (dataSource.isPresent()) {
            try (Connection connection = dataSource.get().getConnection()) {
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
        } else {
            healthStatus.put("database", "UNKNOWN");
        }

        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().get("health-check");
                healthStatus.put("redis", "UP");
            } catch (Exception e) {
                log.error("Redis health check failed", e);
                healthStatus.put("redis", "DOWN");
                healthStatus.put("redisError", e.getMessage());
                healthStatus.put("status", "DOWN");
            }
        } else {
            healthStatus.put("redis", "UNKNOWN");
        }

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
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("service", environment.getProperty("spring.application.name", "application"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("status", "READY");
        readiness.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        boolean allReady = true;

        if (dataSource.isPresent()) {
            try (Connection connection = dataSource.get().getConnection()) {
                readiness.put("database", "READY");
            } catch (SQLException e) {
                readiness.put("database", "NOT_READY");
                readiness.put("databaseError", e.getMessage());
                allReady = false;
            }
        } else {
            readiness.put("database", "UNKNOWN");
        }

        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().get("readiness-check");
                readiness.put("redis", "READY");
            } catch (Exception e) {
                readiness.put("redis", "NOT_READY");
                readiness.put("redisError", e.getMessage());
                allReady = false;
            }
        } else {
            readiness.put("redis", "UNKNOWN");
        }

        readiness.put("overall", allReady ? "READY" : "NOT_READY");
        return allReady ? ResponseEntity.ok(readiness)
            : ResponseEntity.status(503).body(readiness);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        liveness.put("uptime", System.currentTimeMillis());
        return ResponseEntity.ok(liveness);
    }
}
