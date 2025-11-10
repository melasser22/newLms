package com.ejada.tenant.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantCustomerInfo;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingService {

    private final TenantRepository tenantRepository;

    @Transactional
    public void createOrUpdateTenant(final TenantProvisioningEvent event) {
        if (event == null) {
            log.warn("Received null tenant provisioning event");
            return;
        }

        String code = normalize(event.extCustomerId());
        if (!StringUtils.hasText(code)) {
            log.warn("Skipping tenant provisioning event without extCustomerId: {}", event);
            return;
        }

        Tenant tenant = tenantRepository.findByCode(code).orElseGet(() -> {
            Tenant created = new Tenant();
            created.setCode(code);
            return created;
        });

        TenantCustomerInfo customer = event.customerInfo();
        tenant.setName(determineName(customer, code));
        tenant.setContactEmail(truncateNullable(customer == null ? null : customer.email(), Tenant.EMAIL_LENGTH));
        tenant.setContactPhone(truncateNullable(customer == null ? null : customer.mobileNo(), Tenant.PHONE_LENGTH));
        tenant.setActive(Boolean.TRUE);
        tenant.setIsDeleted(Boolean.FALSE);

        tenantRepository.save(tenant);
        log.info("Tenant [{}] upserted from subscription event", tenant.getCode());
    }

    private String determineName(final TenantCustomerInfo customer, final String fallback) {
        if (customer == null) {
            return fallback;
        }
        if (StringUtils.hasText(customer.customerNameEn())) {
            return truncate(customer.customerNameEn(), Tenant.NAME_LENGTH);
        }
        if (StringUtils.hasText(customer.customerNameAr())) {
            return truncate(customer.customerNameAr(), Tenant.NAME_LENGTH);
        }
        return fallback;
    }

    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        return truncate(value.trim(), Tenant.CODE_LENGTH);
    }

    private String truncate(final String value, final int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String truncateNullable(final String value, final int maxLength) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        return truncate(trimmed, maxLength);
    }
}
