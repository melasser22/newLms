package com.ejada.usage.repository;

import com.ejada.usage.domain.UsageAggregate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsageRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public UsageRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate =
        new NamedParameterJdbcTemplate(Objects.requireNonNull(jdbcTemplate).getJdbcTemplate());
  }

  public List<UsageAggregate> loadUsage(String tenantId, LocalDate from, LocalDate to) {
    String sql =
        """
        select date_trunc('day', occurred_at)::date as day,
               sum(case when event_type = 'DELIVERED' then 1 else 0 end) as delivered,
               sum(case when event_type = 'BOUNCED' then 1 else 0 end) as bounced,
               sum(case when event_type = 'SPAM' then 1 else 0 end) as complaints
          from email_event_log
         where tenant_id = :tenantId
           and occurred_at between :from and :to
         group by 1
         order by 1 asc
        """;
    return jdbcTemplate.query(
        sql,
        new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("from", from).addValue("to", to),
        this::mapRow);
  }

  private UsageAggregate mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new UsageAggregate(
        rs.getDate("day").toLocalDate(), rs.getLong("delivered"), rs.getLong("bounced"), rs.getLong("complaints"));
  }
}
