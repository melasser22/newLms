package com.ejada.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactiveContextHolderTest {

    @Test
    void resolvesTenantFromDedicatedContextKey() {
        StepVerifier.create(
                ReactiveContextHolder.getTenantId()
                        .contextWrite(context -> context.put(ReactiveContextHolder.TENANT_CONTEXT_KEY, "tenant-a"))
        )
            .assertNext(tenant -> assertThat(tenant).isEqualTo("tenant-a"))
            .verifyComplete();
    }

    @Test
    void resolvesTenantFromHeaderNameWhenDedicatedKeyMissing() {
        StepVerifier.create(
                ReactiveContextHolder.getTenantId()
                        .contextWrite(context -> context.put(HeaderNames.X_TENANT_ID, "tenant-b"))
        )
            .assertNext(tenant -> assertThat(tenant).isEqualTo("tenant-b"))
            .verifyComplete();
    }

    @Test
    void withTenantPropagatesTenantIdToContext() {
        StepVerifier.create(
                ReactiveContextHolder.withTenant(ReactiveContextHolder.getTenantId(), "tenant-c")
        )
            .assertNext(tenant -> assertThat(tenant).isEqualTo("tenant-c"))
            .verifyComplete();
    }
}
