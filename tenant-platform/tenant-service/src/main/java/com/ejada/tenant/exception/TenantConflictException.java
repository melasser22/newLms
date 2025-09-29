package com.ejada.tenant.exception;

public class TenantConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final TenantErrorCode errorCode;

    public TenantConflictException(final TenantErrorCode errorCode, final String details) {
        super(details == null ? errorCode.getDefaultMessage() : details);
        this.errorCode = errorCode;
    }

    public TenantErrorCode getErrorCode() {
        return errorCode;
    }
}
