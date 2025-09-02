package com.ejada.common.exception;

import com.ejada.common.constants.ErrorCodes;

/**
 * Exception thrown when a requested entity or resource is not found.
 * Example: user by ID, order by reference, file by key, etc.
 */
public class NotFoundException extends SharedException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a NotFoundException with a default message and details.
     *
     * @param details Information about what resource was not found
     */
    public NotFoundException(String details) {
        super(ErrorCodes.NOT_FOUND, "Resource not found", details);
    }

    /**
     * Create a NotFoundException with a custom message and details.
     *
     * @param message Custom not-found message
     * @param details More specific explanation
     */
    public NotFoundException(String message, String details) {
        super(ErrorCodes.NOT_FOUND, message, details);
    }
}
