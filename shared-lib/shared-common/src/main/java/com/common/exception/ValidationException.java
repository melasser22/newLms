package com.common.exception;

import com.common.constants.ErrorCodes;

/**
 * Exception thrown when request validation fails.
 * Example: missing fields, invalid formats, business rule violations.
 */
public class ValidationException extends SharedException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a ValidationException with details.
     *
     * @param details Specific reason of validation failure
     */
    public ValidationException(String details) {
        super(ErrorCodes.VALIDATION_ERROR, "Validation failed", details);
    }

    /**
     * Create a ValidationException with custom message + details.
     *
     * @param message Custom validation message
     * @param details Detailed explanation
     */
    public ValidationException(String message, String details) {
        super(ErrorCodes.VALIDATION_ERROR, message, details);
    }
}
