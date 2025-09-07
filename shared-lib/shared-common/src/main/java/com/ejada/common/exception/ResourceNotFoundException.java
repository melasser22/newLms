package com.ejada.common.exception;

/**
 * Thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Create a ResourceNotFoundException with a formatted message.
     *
     * @param resource the type or name of the resource
     * @param id       the identifier of the missing resource
     */
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }
}

