package com.ejada.common.context;

import com.ejada.common.constants.HeaderNames;
import org.slf4j.MDC;

/**
 * Helper for interacting with the tenant identifier stored inside the SLF4J MDC.
 * Keeping this logic separate from {@link CorrelationContextUtil} avoids
 * accidentally clearing tenant data when only the correlation identifier should
 * be touched.
 */
public final class TenantMdcUtil {

    private TenantMdcUtil() {
        // utility class
    }

    /**
     * Store the given tenant identifier inside MDC using the canonical key.
     * Passing {@code null} clears the entry.
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            MDC.remove(HeaderNames.X_TENANT_ID);
        } else {
            MDC.put(HeaderNames.X_TENANT_ID, tenantId);
        }
    }

    /**
     * @return the tenant identifier currently stored in MDC or {@code null} if missing.
     */
    public static String getTenantId() {
        return MDC.get(HeaderNames.X_TENANT_ID);
    }

    /** Remove the tenant identifier from MDC. */
    public static void clear() {
        MDC.remove(HeaderNames.X_TENANT_ID);
    }
}
