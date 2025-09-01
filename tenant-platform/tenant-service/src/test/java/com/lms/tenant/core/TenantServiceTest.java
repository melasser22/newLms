package com.lms.tenant.core;

import com.lms.tenant.events.publisher.OutboxService;
import com.lms.tenant.events.tenants.OverageToggleChanged;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class TenantServiceTest {

    @Test
    void togglingOverageAppendsEvent() {
        TenantSettingsPort settings = mock(TenantSettingsPort.class);
        OutboxService outbox = mock(OutboxService.class);
        TenantService service = new TenantService(settings, outbox);
        UUID id = UUID.randomUUID();

        service.toggleOverage(id, true);

        verify(settings).setOverageEnabled(id, true);
        verify(outbox).append(new OverageToggleChanged(id, true), anyMap());
    }
}
