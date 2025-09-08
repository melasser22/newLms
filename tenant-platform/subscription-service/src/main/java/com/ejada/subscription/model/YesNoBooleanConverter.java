package com.ejada.subscription.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public final class YesNoBooleanConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(final Boolean value) {
        if (value == null) {
            return "N";
        }
        return value ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(final String dbValue) {
        return "Y".equalsIgnoreCase(dbValue);
    }
}