package com.ejada.analytics.repository;

import com.ejada.analytics.model.UsageSummaryView;
import com.ejada.analytics.model.UsageSummaryViewId;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageSummaryViewRepository
    extends JpaRepository<UsageSummaryView, UsageSummaryViewId> {

  @Query(
      "select v from UsageSummaryView v "
          + "where v.id.tenantId = :tenantId "
          + "and v.id.usagePeriod >= :start "
          + "and v.id.usagePeriod < :end "
          + "order by v.id.usagePeriod desc")
  List<UsageSummaryView> findForTenantAndPeriod(
      @Param("tenantId") Long tenantId,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end);
}
