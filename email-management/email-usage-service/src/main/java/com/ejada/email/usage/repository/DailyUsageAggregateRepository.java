package com.ejada.email.usage.repository;

import com.ejada.email.usage.domain.DailyUsageAggregate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyUsageAggregateRepository extends JpaRepository<DailyUsageAggregate, Long> {

  List<DailyUsageAggregate> findByTenantIdAndUsageDateBetweenOrderByUsageDate(
      String tenantId, LocalDate from, LocalDate to);

  Optional<DailyUsageAggregate> findByTenantIdAndUsageDate(String tenantId, LocalDate usageDate);

  @Query(
      "select coalesce(sum(a.sentCount),0) from DailyUsageAggregate a where a.tenantId = :tenantId and a.usageDate between :from and :to")
  long sumSentBetween(@Param("tenantId") String tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "select coalesce(sum(a.quotaConsumed),0) from DailyUsageAggregate a where a.tenantId = :tenantId and a.usageDate between :from and :to")
  long sumQuotaBetween(@Param("tenantId") String tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
