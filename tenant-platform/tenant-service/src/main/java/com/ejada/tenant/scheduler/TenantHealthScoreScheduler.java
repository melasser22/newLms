package com.ejada.tenant.scheduler;

import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.repository.TenantRepository;
import com.ejada.tenant.service.TenantHealthService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantHealthScoreScheduler {

    private final TenantRepository tenantRepository;
    private final TenantHealthService tenantHealthService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void recalculateDailyScores() {
        List<Tenant> tenants = tenantRepository.findByIsDeletedFalse();
        for (Tenant tenant : tenants) {
            try {
                tenantHealthService.calculateAndStoreHealthScore(tenant.getId());
            } catch (Exception ex) {
                log.error("Failed to calculate health score for tenant {}", tenant.getId(), ex);
            }
        }
    }
}
