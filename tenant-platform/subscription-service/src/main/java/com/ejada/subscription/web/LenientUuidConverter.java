package com.ejada.subscription.web;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Converter that tolerates UUID header values wrapped in curly braces.
 */
@Component
public class LenientUuidConverter implements Converter<String, UUID> {

    @Override
    public UUID convert(@Nullable final String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        String normalized = source.trim();
        if (normalized.startsWith("{") && normalized.endsWith("}")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return UUID.fromString(normalized);
    }
}
