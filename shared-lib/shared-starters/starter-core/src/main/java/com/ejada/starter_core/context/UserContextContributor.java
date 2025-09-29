package com.ejada.starter_core.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Captures the authenticated user identifier (if present) and publishes it to
 * both MDC and {@link ContextManager}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class UserContextContributor implements RequestContextContributor {

    @Override
    public ContextScope contribute(HttpServletRequest request, HttpServletResponse response) {
        String headerUser = trimToNull(request.getHeader(HeaderNames.USER_ID));
        String attributeUser = request.getAttribute(HeaderNames.USER_ID) instanceof String
                ? trimToNull((String) request.getAttribute(HeaderNames.USER_ID))
                : null;
        String userId = firstNonNull(headerUser, attributeUser);
        if (userId == null) {
            return ContextScope.noop();
        }

        MDC.put(HeaderNames.USER_ID, userId);
        ContextManager.setUserId(userId);

        return () -> {
            MDC.remove(HeaderNames.USER_ID);
            ContextManager.clearUserId();
        };
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
