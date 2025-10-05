package com.ejada.subscription.acl.service;

import com.ejada.common.marketplace.token.TokenHashing;
import com.ejada.subscription.model.InboundNotificationAudit;
import com.ejada.subscription.repository.InboundNotificationAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAuditService {

    private final InboundNotificationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final NewTransactionExecutor newTransactionExecutor;

    public InboundNotificationAudit recordInboundAudit(
            final java.util.UUID rqUid, final String token, final Object payload, final String endpoint) {
        final String tokenHash = token == null ? null : TokenHashing.sha256(token);
        final String payloadJson = payload == null ? null : writeJson(payload);

        return newTransactionExecutor.execute(
                () ->
                        auditRepository
                                .findByRqUidAndEndpoint(rqUid, endpoint)
                                .orElseGet(
                                        () -> {
                                            InboundNotificationAudit audit = new InboundNotificationAudit();
                                            audit.setRqUid(rqUid);
                                            audit.setEndpoint(endpoint);
                                            audit.setTokenHash(tokenHash);
                                            audit.setPayload(payloadJson);
                                            try {
                                                return auditRepository.save(audit);
                                            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                                                return auditRepository
                                                        .findByRqUidAndEndpoint(rqUid, endpoint)
                                                        .orElseThrow(() -> ex);
                                            }
                                        }),
                "persist inbound notification audit");
    }

    public void markSuccess(final Long auditId, final String code, final String desc, final String detailsJson) {
        newTransactionExecutor.run(
                () -> auditRepository.markProcessed(auditId, code, desc, detailsJson),
                "mark inbound audit success");
    }

    public void markFailure(final Long auditId, final String code, final String desc, final String detailsJson) {
        newTransactionExecutor.run(
                () -> auditRepository.markProcessed(auditId, code, desc, detailsJson),
                "mark inbound audit failure");
    }

    public void markRollbackOnlyIfActive() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private String writeJson(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize payload for audit", e);
            return "{\"error\":\"serialize\"}";
        }
    }
}
