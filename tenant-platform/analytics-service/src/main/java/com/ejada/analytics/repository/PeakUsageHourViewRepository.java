package com.ejada.analytics.repository;

import com.ejada.analytics.model.PeakUsageHourView;
import com.ejada.analytics.model.PeakUsageHourViewId;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeakUsageHourViewRepository
    extends JpaRepository<PeakUsageHourView, PeakUsageHourViewId> {

  @Query(
      "select v from PeakUsageHourView v "
          + "where v.id.tenantId = :tenantId "
          + "and v.id.usageHour >= :start "
          + "and v.id.usageHour < :end")
  List<PeakUsageHourView> findForTenantBetween(
      @Param("tenantId") Long tenantId,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end);
}
