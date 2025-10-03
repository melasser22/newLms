package com.ejada.subscription.service.approval;

import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.model.Subscription;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RiskAssessmentService {

    public RiskAssessmentResult evaluate(
            final ReceiveSubscriptionNotificationRq request, final Subscription subscription) {
        int score = 0;
        if (subscription.getSubscriptionAmount() != null) {
            BigDecimal amount = subscription.getSubscriptionAmount();
            if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
                score += 35;
            } else if (amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
                score += 25;
            } else if (amount.compareTo(BigDecimal.valueOf(1000)) > 0) {
                score += 15;
            }
        }

        String email = request.customerInfo().email();
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            score += 25;
        } else if (email.endsWith("@gmail.com") || email.endsWith("@yahoo.com")) {
            score += 10;
        }

        if (request.customerInfo().countryCd() == null || request.customerInfo().countryCd().isBlank()) {
            score += 15;
        }

        if (Boolean.TRUE.equals(subscription.getUnlimitedUsersFlag())
                || Boolean.TRUE.equals(subscription.getUnlimitedTransFlag())) {
            score += 10;
        }

        int normalized = Math.min(score, 100);
        String level;
        if (normalized >= 85) {
            level = "CRITICAL";
        } else if (normalized >= 61) {
            level = "HIGH";
        } else if (normalized >= 31) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        return new RiskAssessmentResult(normalized, level, OffsetDateTime.now());
    }

    public record RiskAssessmentResult(int riskScore, String riskLevel, OffsetDateTime evaluatedAt) {}
}
