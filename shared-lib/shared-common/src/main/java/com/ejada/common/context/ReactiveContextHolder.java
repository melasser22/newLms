package com.ejada.common.context;

import com.ejada.common.constants.HeaderNames;
import java.util.Objects;
import reactor.core.publisher.Mono;

/**
 * Helper utilities for working with Reactor's contextual data. Provides
 * convenient accessors to read/write the tenant identifier without directly
 * depending on the thread-local {@link ContextManager} facade.
 */
public final class ReactiveContextHolder {

    /** Key used to store the tenant identifier in the Reactor context. */
    public static final String TENANT_CONTEXT_KEY = "tenantId";

    private ReactiveContextHolder() {
        // utility class
    }

    /**
     * Resolve the tenant identifier from the current Reactor {@code Context}.
     *
     * <p>The lookup first checks the canonical {@link #TENANT_CONTEXT_KEY} and
     * then falls back to the standard {@link HeaderNames#X_TENANT_ID} entry to
     * remain backwards compatible with existing filters.</p>
     *
     * @return a {@link Mono} emitting the tenant identifier when present
     */
    public static Mono<String> getTenantId() {
        return Mono.deferContextual(ctx ->
            Mono.justOrEmpty(
                ctx.<String>getOrEmpty(TENANT_CONTEXT_KEY)
                    .or(() -> ctx.<String>getOrEmpty(HeaderNames.X_TENANT_ID))
            )
        );
    }

    /**
     * Attach the provided tenant identifier to the Reactor {@code Context} of
     * the given publisher.
     *
     * @param source the source publisher to enrich
     * @param tenantId the tenant identifier to propagate
     * @return the enriched publisher
     */
    public static <T> Mono<T> withTenant(Mono<T> source, String tenantId) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(tenantId, "tenantId");
        return source.contextWrite(context ->
            context
                .put(TENANT_CONTEXT_KEY, tenantId)
                .put(HeaderNames.X_TENANT_ID, tenantId)
        );
    }
}
