package com.ejada.subscription.tenant;

import com.ejada.common.marketplace.subscription.dto.CustomerInfoDto;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.tenant.TenantIdentifiers;
import com.ejada.subscription.model.Subscription;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Produces normalized tenant metadata derived from incoming marketplace payloads and
 * stored subscription entities.
 */
@Component
public class TenantLinkFactory {

    private static final int TENANT_CODE_MAX = 64;
    private static final int TENANT_NAME_MAX = 128;

    public TenantLink resolve(
            final ReceiveSubscriptionNotificationRq request, final Subscription subscription) {
        CustomerInfoDto customerInfo = request != null ? request.customerInfo() : null;
        return resolve(customerInfo, subscription);
    }

    public TenantLink resolve(final CustomerInfoDto customerInfo, final Subscription subscription) {

        String tenantCode = sanitizeCode(subscription != null ? subscription.getTenantCode() : null);
        if (!StringUtils.hasText(tenantCode) && subscription != null) {
            tenantCode = deriveDefaultCode(subscription);
        }

        String tenantName = sanitizeName(resolveTenantName(customerInfo, tenantCode));
        UUID securityTenantId = subscription != null && subscription.getSecurityTenantId() != null
                ? subscription.getSecurityTenantId()
                : (StringUtils.hasText(tenantCode) ? TenantIdentifiers.deriveTenantId(tenantCode) : null);

        return new TenantLink(tenantCode, tenantName, securityTenantId);
    }

    private String deriveDefaultCode(final Subscription subscription) {
        Long customerId = subscription.getExtCustomerId();
        String base = customerId != null
                ? "CUST-" + customerId
                : "SUB-" + Optional.ofNullable(subscription.getExtSubscriptionId()).orElse(0L);
        String normalized = base.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "-");
        return sanitizeCode(normalized);
    }

    private String resolveTenantName(final CustomerInfoDto customerInfo, final String tenantCode) {
        if (customerInfo == null) {
            return tenantCode;
        }
        return firstNonBlank(
                customerInfo.customerNameEn(),
                customerInfo.customerNameAr(),
                tenantCode);
    }

    private String sanitizeCode(final String code) {
        return safeTrim(code, TENANT_CODE_MAX);
    }

    private String sanitizeName(final String name) {
        return safeTrim(name, TENANT_NAME_MAX);
    }

    private String safeTrim(final String value, final int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(final String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
