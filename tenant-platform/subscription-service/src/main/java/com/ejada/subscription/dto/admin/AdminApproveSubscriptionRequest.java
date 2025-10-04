package com.ejada.subscription.dto.admin;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/**
 * Request payload used by administrators to approve a pending subscription.
 */
public record AdminApproveSubscriptionRequest(
        @Size(max = 1024) String approvalNotes,
        List<@Size(max = 64) String> additionalChecks,
        Boolean notifyCustomer) {

    public AdminApproveSubscriptionRequest {
        approvalNotes = normalizeNotes(approvalNotes);
        additionalChecks = normalizeChecks(additionalChecks);
        notifyCustomer = Boolean.TRUE.equals(notifyCustomer);
    }

    private static String normalizeNotes(final String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> normalizeChecks(final List<String> checks) {
        if (checks == null || checks.isEmpty()) {
            return List.of();
        }
        return List.copyOf(
                checks.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(entry -> !entry.isEmpty())
                        .toList());
    }

    public boolean shouldNotifyCustomer() {
        return Boolean.TRUE.equals(notifyCustomer);
    }

    @Override
    public List<String> additionalChecks() {
        return List.copyOf(this.additionalChecks);
    }
}
