package com.ejada.catalog.service.impl;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.catalog.model.TenantAddonEntitlement;
import com.ejada.catalog.model.TenantFeatureEntitlement;
import com.ejada.catalog.repository.TenantAddonEntitlementRepository;
import com.ejada.catalog.repository.TenantFeatureEntitlementRepository;
import com.ejada.common.events.provisioning.ProvisionedAddon;
import com.ejada.common.events.provisioning.ProvisionedFeature;
import com.ejada.common.events.provisioning.TenantProvisioningMessage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceImplTest {

    @Mock private TenantFeatureEntitlementRepository featureRepository;
    @Mock private TenantAddonEntitlementRepository addonRepository;

    private TenantProvisioningServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantProvisioningServiceImpl(featureRepository, addonRepository);
    }

    @Test
    void applyProvisioningUpsertsEntitlements() {
        TenantFeatureEntitlement existingFeature = new TenantFeatureEntitlement();
        existingFeature.setTenantCode("TEN-1");
        existingFeature.setFeatureCode("FEATURE_A");
        existingFeature.setFeatureCount(1);

        TenantAddonEntitlement existingAddon = new TenantAddonEntitlement();
        existingAddon.setTenantCode("TEN-1");
        existingAddon.setAddonCode("ADDON_A");

        when(featureRepository.findByTenantCode("TEN-1")).thenReturn(List.of(existingFeature));
        when(addonRepository.findByTenantCode("TEN-1")).thenReturn(List.of(existingAddon));

        TenantProvisioningMessage message = new TenantProvisioningMessage(
                UUID.randomUUID(),
                123L,
                "TEN-1",
                "Tenant",
                List.of(new ProvisionedFeature("FEATURE_A", 5)),
                List.of(new ProvisionedAddon(
                        99L,
                        "ADDON_A",
                        "Addon EN",
                        "Addon AR",
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        "USD",
                        Boolean.TRUE,
                        2L,
                        "ONE_TIME")),
                OffsetDateTime.now());

        service.applyProvisioning(message);

        verify(featureRepository).save(argThat(ent -> ent.getFeatureCode().equals("FEATURE_A")
                && ent.getFeatureCount().equals(5)));

        verify(addonRepository).save(argThat(ent -> ent.getAddonCode().equals("ADDON_A")
                && ent.getRequestedCount().equals(2L)
                && ent.getServicePrice().equals(BigDecimal.TEN)));
    }

    @Test
    void applyProvisioningRemovesMissingEntitlements() {
        TenantFeatureEntitlement staleFeature = new TenantFeatureEntitlement();
        staleFeature.setTenantCode("TEN-1");
        staleFeature.setFeatureCode("FEATURE_OLD");
        when(featureRepository.findByTenantCode("TEN-1")).thenReturn(List.of(staleFeature));

        TenantAddonEntitlement staleAddon = new TenantAddonEntitlement();
        staleAddon.setTenantCode("TEN-1");
        staleAddon.setAddonCode("ADDON_OLD");
        when(addonRepository.findByTenantCode("TEN-1")).thenReturn(List.of(staleAddon));

        TenantProvisioningMessage message = new TenantProvisioningMessage(
                UUID.randomUUID(),
                123L,
                "TEN-1",
                "Tenant",
                List.of(),
                List.of(),
                OffsetDateTime.now());

        service.applyProvisioning(message);

        verify(featureRepository).deleteAll(List.of(staleFeature));
        verify(addonRepository).deleteAll(List.of(staleAddon));
    }
}
