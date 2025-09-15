package com.ejada.common.exception;

import lombok.Getter;
import com.ejada.common.enums.StatusEnums;

/**
 * Base runtime exception for all Shared services.
 * Provides a unified way to carry error codes, messages, and status.
 */
@Getter
public class SharedException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /** Application-specific error code (e.g., ERR-VALIDATION, ERR-NOT-FOUND) */
    private final String errorCode;

    /** HTTP-like status to classify the error */
    private final StatusEnums.ApiStatus status;

    /** Optional details for debugging / client response */
    private final String details;

    // ===== Constructors =====
    public SharedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = StatusEnums.ApiStatus.ERROR;
        this.details = null;
    }

    public SharedException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.status = StatusEnums.ApiStatus.ERROR;
        this.details = details;
    }

    public SharedException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = StatusEnums.ApiStatus.ERROR;
        this.details = null;
    }

    public SharedException(String errorCode, String message, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = StatusEnums.ApiStatus.ERROR;
        this.details = details;
    }
}
