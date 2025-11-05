package com.ejada.admin.exception;


/**
 * Exception thrown when password history requirements cannot be verified.
 */
public class PasswordHistoryUnavailableException extends RuntimeException {

    public PasswordHistoryUnavailableException(String message) {
        super(message);
    }

    public PasswordHistoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}