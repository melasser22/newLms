package com.ejada.tenant.exception;

public enum TenantErrorCode {
    CODE_EXISTS("TENANT_CODE_EXISTS", "Tenant code already exists"),
    NAME_EXISTS("TENANT_NAME_EXISTS", "Tenant name already exists");

    private final String code;
    private final String defaultMessage;

    TenantErrorCode(final String code, final String defaultMessage) {
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
