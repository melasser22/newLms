package com.common.exception;

/**
 * Checked exception indicating a failure during JSON serialization or deserialization.
 *
 * <p>This exception wraps underlying Jackson exceptions and propagates them to
 * callers, encouraging explicit handling instead of silently swallowing
 * {@link RuntimeException}s.</p>
 */
public class JsonSerializationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception with the specified detail message.
     *
     * @param message detail message
     */
    public JsonSerializationException(String message) {
        super(message);
    }

    /**
     * Construct a new exception with the specified detail message and cause.
     *
     * @param message detail message
     * @param cause   underlying cause
     */
    public JsonSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new exception wrapping the given cause.
     *
     * @param cause underlying cause
     */
    public JsonSerializationException(Throwable cause) {
        super(cause);
    }
}