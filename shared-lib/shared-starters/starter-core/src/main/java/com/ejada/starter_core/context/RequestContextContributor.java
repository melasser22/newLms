package com.ejada.starter_core.context;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Strategy interface allowing modules to participate in request context enrichment.
 * Implementations can populate MDC, thread-locals or other context carriers and
 * return a {@link ContextScope} that performs the corresponding cleanup.
 */
@FunctionalInterface
public interface RequestContextContributor {

    /**
     * Apply contribution to the current request/response pair.
     *
     * @return a {@link ContextScope} used to cleanup state after the filter chain completes
     */
    ContextScope contribute(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;

    /**
     * Cleanup handle returned by contributors so the filter can perform deterministic teardown.
     */
    @FunctionalInterface
    interface ContextScope extends AutoCloseable {
        @Override
        void close();

        static ContextScope noop() {
            return () -> { };
        }
    }
}
