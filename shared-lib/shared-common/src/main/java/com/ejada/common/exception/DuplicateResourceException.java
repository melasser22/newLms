package com.ejada.common.exception;

import com.ejada.common.constants.ErrorCodes;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends SharedException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a DuplicateResourceException with resource type and identifier.
     *
     * @param resourceType the type of the resource
     * @param resourceId the identifier of the conflicting resource
     */
    public DuplicateResourceException(String resourceType, String resourceId) {
        super(ErrorCodes.DATA_DUPLICATE,
              String.format("%s with id '%s' already exists", resourceType, resourceId));
    }

    /**
     * Create a DuplicateResourceException with a custom message.
     *
     * @param message custom message describing the conflict
     */
    public DuplicateResourceException(String message) {
        super(ErrorCodes.DATA_DUPLICATE, message);
    }
}
