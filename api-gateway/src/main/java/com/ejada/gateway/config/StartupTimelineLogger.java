package com.ejada.gateway.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.springframework.core.metrics.StartupStep;

/**
 * Emits a concise summary of the startup timeline once the application is ready so we can
 * understand which beans and auto-configurations completed after the
 * {@code "Starting ApiGatewayApplication"} log entry.
 */
@Component
@Lazy(false)
public class StartupTimelineLogger implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StartupTimelineLogger.class);

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    if (!LOGGER.isInfoEnabled()) {
      return;
    }

    if (!(event.getApplicationContext().getApplicationStartup() instanceof BufferingApplicationStartup buffering)) {
      LOGGER.debug("ApplicationStartup does not support buffering; startup timeline logging skipped");
      return;
    }

    StartupTimeline timeline = buffering.drainBufferedTimeline();
    List<StartupTimeline.TimelineEvent> sortedEvents = timeline.getEvents().stream()
        .sorted(Comparator.comparing(StartupTimeline.TimelineEvent::getStartTime))
        .collect(Collectors.toList());

    if (sortedEvents.isEmpty()) {
      LOGGER.info("Spring Boot startup timeline did not record any steps");
      return;
    }

    Instant end = sortedEvents.get(sortedEvents.size() - 1).getEndTime();
    Duration total = Duration.between(timeline.getStartTime(), end);
    LOGGER.info("Spring Boot startup captured {} steps over {} ms", sortedEvents.size(), total.toMillis());

    sortedEvents.stream()
        .filter(eventStep -> shouldLog(eventStep.getStartupStep()))
        .forEach(eventStep -> logStep(eventStep));
  }

  private void logStep(StartupTimeline.TimelineEvent event) {
    StartupStep step = event.getStartupStep();
    String stepName = step.getName();
    Duration duration = event.getDuration();
    if (stepName.startsWith("spring.boot.autoconfigure")) {
      String autoConfiguration = tagValue(step, "className");
      LOGGER.info("  Auto-configuration {} completed in {} ms",
          (autoConfiguration != null) ? autoConfiguration : stepName,
          duration.toMillis());
      return;
    }

    if (stepName.startsWith("spring.beans.")) {
      String beanName = tagValue(step, "beanName");
      String beanType = tagValue(step, "beanType");
      if (beanType != null) {
        LOGGER.info("  Bean {} [{}] initialised in {} ms",
            (beanName != null) ? beanName : "<unknown>",
            beanType,
            duration.toMillis());
      } else {
        LOGGER.info("  Bean {} initialised in {} ms", (beanName != null) ? beanName : stepName, duration.toMillis());
      }
    }
  }

  private boolean shouldLog(StartupStep step) {
    String name = step.getName();
    if (name.startsWith("spring.boot.autoconfigure")) {
      return true;
    }
    if (name.startsWith("spring.beans.")) {
      String beanType = tagValue(step, "beanType");
      return beanType != null
          && (beanType.startsWith("com.ejada.gateway")
              || beanType.startsWith("org.springframework.kafka")
              || beanType.startsWith("org.springframework.data.redis"));
    }
    return false;
  }

  private String tagValue(StartupStep step, String tagName) {
    for (StartupStep.Tag tag : step.getTags()) {
      if (tagName.equals(tag.getKey())) {
        return tag.getValue();
      }
    }
    return null;
  }
}

