package com.ejada.common.events.tenant;

import java.io.Serializable;

/**
 * Event published by the subscription service to kick off tenant onboarding in
 * downstream services.
 */
public record TenantProvisioningEvent(
        Long subscriptionId,
        String extSubscriptionId,
        String extCustomerId,
        TenantCustomerInfo customerInfo,
        TenantAdminInfo adminInfo
) implements Serializable {

    public TenantProvisioningEvent {
        if (extCustomerId != null) {
            extCustomerId = extCustomerId.trim();
        }
    }

    /** Customer details forwarded from the marketplace payload. */
    public record TenantCustomerInfo(
            String customerNameEn,
            String customerNameAr,
            String customerType,
            String crNumber,
            String countryCd,
            String cityCd,
            String addressEn,
            String addressAr,
            String email,
            String mobileNo
    ) implements Serializable { }

    /** Admin user details forwarded from the marketplace payload. */
    public record TenantAdminInfo(
            String adminUserName,
            String email,
            String mobileNo,
            String preferredLang
    ) implements Serializable { }
}
