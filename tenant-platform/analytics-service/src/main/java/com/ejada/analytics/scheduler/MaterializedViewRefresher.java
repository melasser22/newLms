package com.ejada.analytics.scheduler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaterializedViewRefresher {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaterializedViewRefresher.class);

  private static final List<String> MATERIALIZED_VIEWS =
      List.of(
          "mv_tenant_feature_usage_daily",
          "mv_tenant_usage_summary",
          "mv_tenant_peak_usage_hourly");

  private final JdbcTemplate jdbcTemplate;
  private final String cronExpression;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "JdbcTemplate is a managed Spring bean and is safe to retain")
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
    MATERIALIZED_VIEWS.forEach(this::refreshMaterializedView);
  }

  private void refreshMaterializedView(String viewName) {
    Boolean exists =
        jdbcTemplate.queryForObject(
            "select exists (select 1 from pg_matviews where schemaname = current_schema() and matviewname = ?)",
            Boolean.class,
            viewName);

    if (!Boolean.TRUE.equals(exists)) {
      LOGGER.debug("Skipping refresh for materialized view '{}' because it does not exist", viewName);
      return;
    }

    try {
      jdbcTemplate.execute("refresh materialized view " + viewName);
    } catch (DataAccessException ex) {
      LOGGER.warn("Failed to refresh materialized view '{}'", viewName, ex);
    }
  }
}
