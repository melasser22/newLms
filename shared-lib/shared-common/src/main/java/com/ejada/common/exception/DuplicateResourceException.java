package com.ejada.common.exception;

/**
 * Thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Create a new DuplicateResourceException with the given message.
     *
     * @param message detail message describing the duplicate
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}

