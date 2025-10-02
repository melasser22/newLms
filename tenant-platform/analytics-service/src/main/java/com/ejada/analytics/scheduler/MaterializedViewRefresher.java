package com.ejada.analytics.scheduler;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaterializedViewRefresher {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaterializedViewRefresher.class);

  private final JdbcTemplate jdbcTemplate;
  private final String cronExpression;

  public MaterializedViewRefresher(
      JdbcTemplate jdbcTemplate, @Value("${app.analytics.refresh-cron}") String cronExpression) {
    this.jdbcTemplate = jdbcTemplate;
    this.cronExpression = cronExpression;
  }

  @PostConstruct
  public void onStartup() {
    refreshViews();
  }

  @Scheduled(cron = "${app.analytics.refresh-cron}")
  public void scheduledRefresh() {
    refreshViews();
  }

  private void refreshViews() {
    LOGGER.debug("Refreshing analytics materialized views (cron={})", cronExpression);
    jdbcTemplate.execute("refresh materialized view if exists mv_tenant_feature_usage_daily");
    jdbcTemplate.execute("refresh materialized view if exists mv_tenant_usage_summary");
    jdbcTemplate.execute("refresh materialized view if exists mv_tenant_peak_usage_hourly");
  }
}
