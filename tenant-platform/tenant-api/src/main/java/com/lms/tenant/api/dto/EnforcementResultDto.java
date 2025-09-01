package com.lms.tenant.api.dto;

/**
 * Outcome of enforcing feature usage against policy constraints.
 *
 * @param allowed whether the action is permitted
 * @param message optional explanatory message
 * @param overage details about any overage that occurred
 */
public record EnforcementResultDto(boolean allowed, String message, OverageDto overage) {
}

