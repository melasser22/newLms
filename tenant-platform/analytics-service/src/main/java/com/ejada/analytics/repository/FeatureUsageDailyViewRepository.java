package com.ejada.analytics.repository;

import com.ejada.analytics.model.FeatureUsageDailyView;
import com.ejada.analytics.model.FeatureUsageDailyViewId;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureUsageDailyViewRepository
    extends JpaRepository<FeatureUsageDailyView, FeatureUsageDailyViewId> {

  @Query(
      "select v from FeatureUsageDailyView v "
          + "where v.id.tenantId = :tenantId "
          + "and v.id.usageDay >= :start "
          + "and v.id.usageDay < :end")
  List<FeatureUsageDailyView> findForTenantBetween(
      @Param("tenantId") Long tenantId,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end);
}
