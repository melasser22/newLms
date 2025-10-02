package com.ejada.tenant.service.health;

public record TenantHealthMetrics(
        double featureAdoptionRate,
        double loginFrequencyScore,
        double userEngagementScore,
        double usageTrendScore,
        double supportTicketScore,
        double paymentHistoryScore,
        double apiHealthScore
) {

    public static TenantHealthMetrics empty() {
        return new TenantHealthMetrics(0, 0, 0, 0, 0, 0, 0);
    }
}
