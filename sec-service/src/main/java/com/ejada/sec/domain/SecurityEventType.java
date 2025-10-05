package com.ejada.sec.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of security events tracked in the system.
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventType {
    // Authentication Events
    LOGIN_SUCCESS("Successful login"),
    LOGIN_FAILURE("Failed login attempt"),
    LOGOUT("User logout"),

    // MFA Events
    MFA_ENABLED("MFA activated"),
    MFA_DISABLED("MFA deactivated"),
    MFA_SUCCESS("MFA verification successful"),
    MFA_FAILURE("MFA verification failed"),
    MFA_BACKUP_CODE_USED("Backup code used"),

    // Password Events
    PASSWORD_CHANGE("Password changed"),
    PASSWORD_RESET_REQUESTED("Password reset requested"),
    PASSWORD_RESET_COMPLETED("Password reset completed"),

    // Authorization Events
    ROLE_ASSIGNED("Role assigned to user"),
    ROLE_REVOKED("Role revoked from user"),
    PRIVILEGE_GRANTED("Privilege granted"),
    PRIVILEGE_REVOKED("Privilege revoked"),
    PRIVILEGE_OVERRIDE_SET("User privilege override set"),

    // Account Management
    ACCOUNT_CREATED("User account created"),
    ACCOUNT_UPDATED("User account updated"),
    ACCOUNT_DELETED("User account deleted"),
    ACCOUNT_ENABLED("Account enabled"),
    ACCOUNT_DISABLED("Account disabled"),
    ACCOUNT_LOCKED("Account locked"),
    ACCOUNT_UNLOCKED("Account unlocked"),

    // Security Threats
    BRUTE_FORCE_DETECTED("Brute force attempt detected"),
    SUSPICIOUS_LOCATION("Login from unusual location"),
    CONCURRENT_SESSION_ANOMALY("Unusual concurrent sessions"),
    PRIVILEGE_ESCALATION_ATTEMPT("Privilege escalation attempted"),
    UNAUTHORIZED_ACCESS_ATTEMPT("Unauthorized access attempted"),
    TOKEN_THEFT_SUSPECTED("Possible token theft"),
    UNUSUAL_TIME_ACCESS("Access during unusual hours"),
    MASS_DATA_EXPORT("Large data export detected"),

    // API Security
    RATE_LIMIT_EXCEEDED("API rate limit exceeded"),
    INVALID_TOKEN("Invalid or expired token used"),
    CSRF_DETECTED("Potential CSRF attack detected");

    private final String description;
}
