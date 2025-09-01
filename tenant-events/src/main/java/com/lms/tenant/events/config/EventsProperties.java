package com.lms.tenant.events.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lms.events")
public class EventsProperties {
  /** base topic prefix (final topic: <topicPrefix>.<eventType>), e.g. lms.tenant */
  private String topicPrefix = "lms.tenant";
  /** batch size for outbox publisher */
  private int batchSize = 200;
  /** max attempts before marking DEAD */
  private int maxAttempts = 10;
  /** schedule delay (ISO-8601), default PT1S */
  private String schedule = "PT1S";
  /** cleanup after days */
  private int cleanupAfterDays = 14;

  public String getTopicPrefix() { return topicPrefix; }
  public void setTopicPrefix(String topicPrefix) { this.topicPrefix = topicPrefix; }
  public int getBatchSize() { return batchSize; }
  public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
  public int getMaxAttempts() { return maxAttempts; }
  public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
  public String getSchedule() { return schedule; }
  public void setSchedule(String schedule) { this.schedule = schedule; }
  public int getCleanupAfterDays() { return cleanupAfterDays; }
  public void setCleanupAfterDays(int cleanupAfterDays) { this.cleanupAfterDays = cleanupAfterDays; }
}
