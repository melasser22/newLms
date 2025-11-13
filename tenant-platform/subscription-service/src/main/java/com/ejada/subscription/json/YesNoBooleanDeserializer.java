package com.ejada.subscription.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import jakarta.annotation.Nullable;
import java.io.IOException;

/**
 * Jackson deserializer that accepts either boolean literals or "Y"/"N" strings and
 * normalizes them to {@link Boolean} values.
 */
public class YesNoBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    @Nullable
    public Boolean deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (token == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null) {
                return null;
            }
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return null;
            }
            if ("Y".equalsIgnoreCase(normalized) || "YES".equalsIgnoreCase(normalized)) {
                return Boolean.TRUE;
            }
            if ("N".equalsIgnoreCase(normalized) || "NO".equalsIgnoreCase(normalized)) {
                return Boolean.FALSE;
            }
            if ("TRUE".equalsIgnoreCase(normalized)) {
                return Boolean.TRUE;
            }
            if ("FALSE".equalsIgnoreCase(normalized)) {
                return Boolean.FALSE;
            }
            return (Boolean)
                    ctxt.handleWeirdStringValue(Boolean.class, normalized, "Expected 'Y' or 'N' (case-insensitive)");
        }
        return (Boolean) ctxt.handleUnexpectedToken(Boolean.class, p);
    }
}
