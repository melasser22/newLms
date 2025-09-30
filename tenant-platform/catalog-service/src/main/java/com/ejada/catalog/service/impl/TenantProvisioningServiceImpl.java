package com.ejada.catalog.service.impl;

import com.ejada.catalog.model.TenantAddonEntitlement;
import com.ejada.catalog.model.TenantFeatureEntitlement;
import com.ejada.catalog.repository.TenantAddonEntitlementRepository;
import com.ejada.catalog.repository.TenantFeatureEntitlementRepository;
import com.ejada.catalog.service.TenantProvisioningService;
import com.ejada.common.events.provisioning.ProvisionedAddon;
import com.ejada.common.events.provisioning.ProvisionedFeature;
import com.ejada.common.events.provisioning.TenantProvisioningMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningServiceImpl implements TenantProvisioningService {

    private final TenantFeatureEntitlementRepository featureRepository;
    private final TenantAddonEntitlementRepository addonRepository;

    @Override
    @Transactional
    public void applyProvisioning(final TenantProvisioningMessage message) {
        if (message == null || message.tenantCode() == null) {
            log.warn("Ignoring provisioning message with missing tenant code");
            return;
        }

        synchronizeFeatures(message.tenantCode(), message.features());
        synchronizeAddons(message.tenantCode(), message.addons());
    }

    private void synchronizeFeatures(final String tenantCode, final List<ProvisionedFeature> features) {
        Map<String, TenantFeatureEntitlement> existing = new HashMap<>();
        for (TenantFeatureEntitlement entitlement : featureRepository.findByTenantCode(tenantCode)) {
            existing.put(entitlement.getFeatureCode(), entitlement);
        }

        if (features != null) {
            for (ProvisionedFeature feature : features) {
                if (feature == null || feature.code() == null) {
                    continue;
                }
                TenantFeatureEntitlement entitlement = existing.remove(feature.code());
                if (entitlement == null) {
                    entitlement = new TenantFeatureEntitlement();
                    entitlement.setTenantCode(tenantCode);
                    entitlement.setFeatureCode(feature.code());
                }
                entitlement.setFeatureCount(feature.quantity());
                featureRepository.save(entitlement);
            }
        }

        if (!existing.isEmpty()) {
            featureRepository.deleteAll(List.copyOf(existing.values()));
        }
    }

    private void synchronizeAddons(final String tenantCode, final List<ProvisionedAddon> addons) {
        Map<String, TenantAddonEntitlement> existing = new HashMap<>();
        for (TenantAddonEntitlement entitlement : addonRepository.findByTenantCode(tenantCode)) {
            existing.put(entitlement.getAddonCode(), entitlement);
        }

        if (addons != null) {
            for (ProvisionedAddon addon : addons) {
                if (addon == null || addon.code() == null) {
                    continue;
                }
                TenantAddonEntitlement entitlement = existing.remove(addon.code());
                if (entitlement == null) {
                    entitlement = new TenantAddonEntitlement();
                    entitlement.setTenantCode(tenantCode);
                    entitlement.setAddonCode(addon.code());
                }
                entitlement.setProductAdditionalServiceId(addon.productAdditionalServiceId());
                entitlement.setServiceNameEn(addon.nameEn());
                entitlement.setServiceNameAr(addon.nameAr());
                entitlement.setServicePrice(addon.servicePrice());
                entitlement.setTotalAmount(addon.totalAmount());
                entitlement.setCurrency(addon.currency());
                entitlement.setCountable(addon.countable());
                entitlement.setRequestedCount(addon.requestedCount());
                entitlement.setPaymentTypeCd(addon.paymentType());
                addonRepository.save(entitlement);
            }
        }

        if (!existing.isEmpty()) {
            addonRepository.deleteAll(List.copyOf(existing.values()));
        }
    }
}
