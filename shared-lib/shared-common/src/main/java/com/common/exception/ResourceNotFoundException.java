package com.common.exception;

/**
 * Exception thrown when a specific resource is not found.
 * This is a more specific version of NotFoundException for resource-related scenarios.
 * Example: user by ID, order by reference, file by key, etc.
 */
public class ResourceNotFoundException extends NotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a ResourceNotFoundException with a default message.
     *
     * @param resourceType the type of resource that was not found
     * @param resourceId the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
    }

    /**
     * Create a ResourceNotFoundException with a custom message.
     *
     * @param message custom not-found message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Create a ResourceNotFoundException with a custom message and details.
     *
     * @param message custom not-found message
     * @param details more specific explanation
     */
    public ResourceNotFoundException(String message, String details) {
        super(message, details);
    }
}
