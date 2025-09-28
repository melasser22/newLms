package com.ejada.sec.exception;

/**
 * Indicates that the platform could not verify a superadmin's password reuse history.
 *
 * <p>This typically happens when the password history table has not been
 * provisioned yet or the underlying data source is unavailable. The
 * exception allows higher layers to surface a clear error message instead of
 * returning a generic 500 error.
 */
public class PasswordHistoryUnavailableException extends RuntimeException {

    public PasswordHistoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
