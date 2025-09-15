package com.ejada.common.exception;

import com.ejada.common.constants.ErrorCodes;

/**
 * Thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends SharedException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new DuplicateResourceException with the default error code.
     *
     * @param message detail message describing the duplicate
     */
    public DuplicateResourceException(String message) {
        super(ErrorCodes.DATA_DUPLICATE, message);
    }

    /**
     * Create a new DuplicateResourceException with additional details.
     *
     * @param message detail message describing the duplicate
     * @param details more specific information about the violation
     */
    public DuplicateResourceException(String message, String details) {
        super(ErrorCodes.DATA_DUPLICATE, message, details);
    }
}

