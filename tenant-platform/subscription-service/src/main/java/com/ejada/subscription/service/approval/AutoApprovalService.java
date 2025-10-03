package com.ejada.subscription.service.approval;

import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionApprovalRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AutoApprovalService {

    public AutoApprovalDecision evaluate(
            final Subscription subscription, final SubscriptionApprovalRequest request) {
        int riskScore = Optional.ofNullable(request.getRiskScore()).orElse(0);
        BigDecimal amount = Optional.ofNullable(subscription.getSubscriptionAmount()).orElse(BigDecimal.ZERO);

        if (amount.compareTo(BigDecimal.valueOf(100)) <= 0 && riskScore < 30) {
            return new AutoApprovalDecision(true, "LOW_VALUE");
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0 && riskScore < 50) {
            return new AutoApprovalDecision(true, "TRIAL");
        }

        return AutoApprovalDecision.NOT_APPROVED;
    }

    public record AutoApprovalDecision(boolean shouldApprove, String ruleTriggered) {
        public static final AutoApprovalDecision NOT_APPROVED = new AutoApprovalDecision(false, null);
    }
}
