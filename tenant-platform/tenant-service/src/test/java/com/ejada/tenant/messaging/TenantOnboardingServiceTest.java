package com.ejada.tenant.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantCustomerInfo;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.repository.TenantRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantOnboardingServiceTest {

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantOnboardingService service = new TenantOnboardingService(tenantRepository);

    @Test
    void createsTenantWhenNotExists() {
        when(tenantRepository.findByCode("CUST-1")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantProvisioningEvent event = new TenantProvisioningEvent(
                42L,
                "SUB-42",
                "CUST-1",
                new TenantCustomerInfo("Ejada EN", "Ejada AR", "COMPANY", null, null, null, null, null,
                        "ops@example.com", "+966500000000"),
                null);

        service.createOrUpdateTenant(event);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("CUST-1");
        assertThat(saved.getName()).isEqualTo("Ejada EN");
        assertThat(saved.getContactEmail()).isEqualTo("ops@example.com");
        assertThat(saved.getContactPhone()).isEqualTo("+966500000000");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void updatesExistingTenantAndReactivates() {
        Tenant existing = new Tenant();
        existing.setCode("CUST-2");
        existing.setName("Old Name");
        existing.setContactEmail("old@example.com");
        existing.setContactPhone("1111");
        existing.setActive(false);
        existing.setIsDeleted(true);

        when(tenantRepository.findByCode("CUST-2")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        TenantProvisioningEvent event = new TenantProvisioningEvent(
                77L,
                "SUB-77",
                "CUST-2",
                new TenantCustomerInfo(null, "Ejada Arabic", null, null, null, null, null, null,
                        "new@example.com", null),
                null);

        service.createOrUpdateTenant(event);

        assertThat(existing.getName()).isEqualTo("Ejada Arabic");
        assertThat(existing.getContactEmail()).isEqualTo("new@example.com");
        assertThat(existing.getContactPhone()).isNull();
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.isDeleted()).isFalse();
        verify(tenantRepository).save(existing);
    }

    @Test
    void skipsEventWithoutCustomerId() {
        TenantProvisioningEvent event = new TenantProvisioningEvent(
                1L,
                "SUB-1",
                "  ",
                new TenantCustomerInfo(null, null, null, null, null, null, null, null, null, null),
                null);

        service.createOrUpdateTenant(event);

        verify(tenantRepository, never()).save(any(Tenant.class));
    }
}
