package com.ejada.starter_core.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.ejada.common.constants.HeaderNames;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;

/**
 * Auto-configures logging for Shared services:
 *  - Adds MDC correlationId/tenantId (from CoreAutoConfiguration filters).
 *  - Optionally enables JSON/Logstash output.
 */
@AutoConfiguration
public class LoggingAutoConfiguration {

    @PostConstruct
    public void init() {
        // Confirm Logback is active
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext ctx) {
            // Basic console appender (fallback if none configured)
            if (ctx.getLogger("ROOT").getAppender("CONSOLE") == null) {
                PatternLayoutEncoder ple = new PatternLayoutEncoder();
                ple.setContext(ctx);
                ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger - %msg %X{" + HeaderNames.CORRELATION_ID + "} %X{tenantId}%n");
                ple.start();

                ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
                consoleAppender.setContext(ctx);
                consoleAppender.setEncoder(ple);
                consoleAppender.setName("CONSOLE");
                consoleAppender.start();

                ctx.getLogger("ROOT").addAppender(consoleAppender);
            }
        }
    }

    /**
     * Optional Logstash/JSON encoder for structured logs.
     * Activate with: `shared.logging.json.enabled=true`
     */
    @Bean
    @ConditionalOnProperty(prefix = "shared.logging.json", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public LogstashEncoder logstashEncoder() {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setIncludeMdc(true);
        encoder.setCustomFields("{\"app\":\"shared\"}");
        return encoder;
    }
}
