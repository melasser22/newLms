package com.ejada.catalog.exception;

public enum CatalogErrorCode {
    TIER_CODE_EXISTS("CAT-409-001", "Tier code already exists");

    private final String code;
    private final String defaultMessage;

    CatalogErrorCode(final String code, final String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
