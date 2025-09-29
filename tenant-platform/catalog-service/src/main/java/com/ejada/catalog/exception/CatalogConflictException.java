package com.ejada.catalog.exception;

public class CatalogConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CatalogErrorCode errorCode;

    public CatalogConflictException(final CatalogErrorCode errorCode, final String message) {
        super(message == null ? errorCode.getDefaultMessage() : message);
        this.errorCode = errorCode;
    }

    public CatalogErrorCode getErrorCode() {
        return errorCode;
    }
}
