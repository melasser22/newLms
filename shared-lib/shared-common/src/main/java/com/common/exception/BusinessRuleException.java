package com.common.exception;

import com.common.constants.ErrorCodes;

/**
 * Exception thrown when a business rule is violated.
 * Example: exceeding credit limit, duplicate booking,
 * invalid state transition, etc.
 */
public class BusinessRuleException extends SharedException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a BusinessRuleException with details.
     *
     * @param details Specific reason of the business rule violation
     */
    public BusinessRuleException(String details) {
        super(ErrorCodes.BUSINESS_RULE_VIOLATION, "Business rule violated", details);
    }

    /**
     * Create a BusinessRuleException with custom message + details.
     *
     * @param message Custom business rule violation message
     * @param details Detailed explanation
     */
    public BusinessRuleException(String message, String details) {
        super(ErrorCodes.BUSINESS_RULE_VIOLATION, message, details);
    }
}
