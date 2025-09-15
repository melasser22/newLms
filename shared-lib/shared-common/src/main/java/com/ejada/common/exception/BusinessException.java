package com.ejada.common.exception;

/**
 * Exception thrown when a business rule is violated.
 * This is a more general version of BusinessRuleException for business logic scenarios.
 * Example: insufficient funds, invalid state transitions, etc.
 */
public class BusinessException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a BusinessException with a default message.
     *
     * @param message the business rule violation message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Create a BusinessException with a custom message and details.
     *
     * @param message custom business rule violation message
     * @param details more specific explanation
     */
    public BusinessException(String message, String details) {
        super(message, details);
    }
}
