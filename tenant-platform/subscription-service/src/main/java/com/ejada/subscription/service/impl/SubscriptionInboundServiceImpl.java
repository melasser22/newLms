package com.ejada.subscription.service.impl;

import com.ejada.subscription.dto.*;
import com.ejada.subscription.mapper.*;
import com.ejada.subscription.dto.SubscriptionUpdateType; // <-- your enum package
import com.ejada.subscription.model.*;
import com.ejada.subscription.repository.*;
import com.ejada.subscription.service.SubscriptionInboundService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Handles marketplace callbacks:
 *  - POST /subscription/receiveSubscriptionNotification
 *  - POST /subscription/receiveSubscriptionUpdate
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionInboundServiceImpl implements SubscriptionInboundService {

    // Repositories
    private final SubscriptionRepository subscriptionRepo;
    private final SubscriptionFeatureRepository featureRepo;
    private final SubscriptionAdditionalServiceRepository additionalServiceRepo;
    private final SubscriptionProductPropertyRepository propertyRepo;
    private final SubscriptionEnvironmentIdentifierRepository envIdRepo;

    private final InboundNotificationAuditRepository auditRepo;
    private final SubscriptionUpdateEventRepository updateEventRepo;
    private final OutboxEventRepository outboxRepo;
    @SuppressWarnings("unused")
    private final IdempotentRequestRepository idemRepo; // optional second guard by rqUID

    // Mappers
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionFeatureMapper featureMapper;
    private final SubscriptionAdditionalServiceMapper additionalServiceMapper;
    private final SubscriptionProductPropertyMapper propertyMapper;
    private final SubscriptionEnvironmentIdentifierMapper envIdMapper;
    private final SubscriptionUpdateEventMapper updateEventMapper;

    // JSON
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final ObjectMapper objectMapper;

    private static final String EP_NOTIFICATION = "RECEIVE_NOTIFICATION";
    private static final String EP_UPDATE       = "RECEIVE_UPDATE";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ServiceResult<ReceiveSubscriptionNotificationRs> receiveSubscriptionNotification(
            final UUID rqUid,
            final String token,
            final ReceiveSubscriptionNotificationRq rq) {

        // 1) Idempotency shortcut (replay same response)
        var existingAudit = auditRepo.findByRqUidAndEndpoint(rqUid, EP_NOTIFICATION).orElse(null);
        if (existingAudit != null && Boolean.TRUE.equals(existingAudit.getProcessed())) {
            var maybeSub = subscriptionRepo.findByExtSubscriptionIdAndExtCustomerId(
                    rq.subscriptionInfo().subscriptionId(), rq.subscriptionInfo().customerId());
            List<SubscriptionEnvironmentIdentifier> ids = maybeSub
                    .map(s -> envIdRepo.findBySubscription_SubscriptionId(s.getSubscriptionId()))
                    .orElseGet(List::of);
            var rs = new ReceiveSubscriptionNotificationRs(Boolean.TRUE, envIdMapper.toDtoList(ids));
            return okNotification(rs);
        }

        // 2) Persist inbound audit row
        InboundNotificationAudit audit = new InboundNotificationAudit();
        audit.setRqUid(rqUid);
        audit.setEndpoint(EP_NOTIFICATION);
        audit.setTokenHash(sha256(token));
        audit.setPayload(writeJson(rq));
        audit = auditRepo.save(audit);

        try {
            // 3) Upsert subscription
            SubscriptionInfoDto si = rq.subscriptionInfo();
            Subscription sub = subscriptionRepo
                    .findByExtSubscriptionIdAndExtCustomerId(si.subscriptionId(), si.customerId())
                    .orElse(null);

            if (sub == null) sub = subscriptionMapper.toEntity(si);
            else             subscriptionMapper.update(sub, si);

            if (sub.getEndDt() == null) sub.setEndDt(Optional.ofNullable(si.endDt()).orElse(LocalDate.now()));
            sub = subscriptionRepo.save(sub);

            // 4) Replace children from payload
            replaceFeatures(sub, si.subscriptionFeatureLst());
            replaceAdditionalServices(sub, si.subscriptionAdditionalServicesLst());
            replaceProductProperties(sub, rq.productProperties());

            // 5) (Optional) environment identifiers (if provisioning already occurred)
            List<SubscriptionEnvironmentIdentifier> envIds =
                    envIdRepo.findBySubscription_SubscriptionId(sub.getSubscriptionId());

            // 6) Mark success and emit outbox
            markAuditSuccess(audit.getInboundNotificationAuditId(), "I000000", "Successful Operation", null);
            emitOutbox("SUBSCRIPTION", sub.getSubscriptionId().toString(), "CREATED_OR_UPDATED",
                    Map.of("extSubscriptionId", sub.getExtSubscriptionId(), "extCustomerId", sub.getExtCustomerId()));

            var rs = new ReceiveSubscriptionNotificationRs(Boolean.TRUE, envIdMapper.toDtoList(envIds));
            return okNotification(rs);

        } catch (Exception ex) {
            log.error("receiveSubscriptionNotification failed", ex);
            markAuditFailure(audit.getInboundNotificationAuditId(), "EINT000", "Unexpected Error",
                    jsonMsg(ex.getMessage()));
            return err("EINT000", "Unexpected Error", jsonMsg("processing failed"));
        }
    }

    @Override
    @Transactional
    public ServiceResult<Void> receiveSubscriptionUpdate(
            final UUID rqUid,
            final String token,
            final ReceiveSubscriptionUpdateRq rq) {

        // 1) Idempotency by rqUID
        if (updateEventRepo.findByRqUid(rqUid).isPresent()) {
            return okVoid();
        }

        // 2) Audit row
        InboundNotificationAudit audit = new InboundNotificationAudit();
        audit.setRqUid(rqUid);
        audit.setEndpoint(EP_UPDATE);
        audit.setTokenHash(sha256(token));
        audit.setPayload(writeJson(rq));
        audit = auditRepo.save(audit);

        try {
            // 3) Persist raw update event (for trace)
            var event = updateEventMapper.toEvent(rq, rqUid);
            event = updateEventRepo.save(event);

            // 4) Lookup subscription and transition status
            Subscription sub = subscriptionRepo.findByExtSubscriptionId(rq.subscriptionId())
                    .orElseThrow(() -> new EntityNotFoundException("Unknown subscriptionId: " + rq.subscriptionId()));

            // Support both enum and string in DTO
            transitionStatus(sub, rq.subscriptionUpdateType());
            subscriptionRepo.save(sub);

            // 5) Mark event processed, audit + outbox
            event.setProcessed(true);
            event.setProcessedAt(OffsetDateTime.now());

            markAuditSuccess(audit.getInboundNotificationAuditId(), "I000000", "Successful Operation", null);
            emitOutbox("SUBSCRIPTION", sub.getSubscriptionId().toString(), "STATUS_CHANGED",
                    Map.of("newStatus", sub.getSubscriptionSttsCd()));

            return okVoid();

        } catch (EntityNotFoundException nf) {
            markAuditFailure(audit.getInboundNotificationAuditId(), "EINT000", "Unexpected Error",
                    jsonMsg(nf.getMessage()));
            return err("EINT000", "Unexpected Error", jsonMsg("subscription not found for update"));

        } catch (Exception ex) {
            log.error("receiveSubscriptionUpdate failed", ex);
            markAuditFailure(audit.getInboundNotificationAuditId(), "EINT000", "Unexpected Error",
                    jsonMsg(ex.getMessage()));
            return err("EINT000", "Unexpected Error", jsonMsg("processing failed"));
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void replaceFeatures(Subscription sub, List<SubscriptionFeatureDto> dtos) {
        var existing = featureRepo.findBySubscription_SubscriptionId(sub.getSubscriptionId());
        if (!existing.isEmpty()) featureRepo.deleteAllInBatch(existing);
        if (dtos != null && !dtos.isEmpty()) {
            var mapped = new ArrayList<SubscriptionFeature>(dtos.size());
            for (var d : dtos) mapped.add(featureMapper.toEntity(d, sub));
            featureRepo.saveAll(mapped);
        }
    }

    private void replaceAdditionalServices(Subscription sub, List<SubscriptionAdditionalServiceDto> dtos) {
        var existing = additionalServiceRepo.findBySubscription_SubscriptionId(sub.getSubscriptionId());
        if (!existing.isEmpty()) additionalServiceRepo.deleteAllInBatch(existing);
        if (dtos != null && !dtos.isEmpty()) {
            var mapped = new ArrayList<SubscriptionAdditionalService>(dtos.size());
            for (var d : dtos) mapped.add(additionalServiceMapper.toEntity(d, sub));
            additionalServiceRepo.saveAll(mapped);
        }
    }

    private void replaceProductProperties(Subscription sub, List<ProductPropertyDto> dtos) {
        var existing = propertyRepo.findBySubscription_SubscriptionId(sub.getSubscriptionId());
        if (!existing.isEmpty()) propertyRepo.deleteAllInBatch(existing);
        if (dtos != null && !dtos.isEmpty()) {
            var mapped = new ArrayList<SubscriptionProductProperty>(dtos.size());
            for (var d : dtos) mapped.add(propertyMapper.toEntity(d, sub));
            propertyRepo.saveAll(mapped);
        }
    }

    // String-based version (kept for flexibility)
    private void transitionStatus(Subscription sub, String updateType) {
        switch (updateType) {
            case "SUSPENDED" -> sub.setSubscriptionSttsCd("SUSPENDED");
            case "RESUMED"   -> sub.setSubscriptionSttsCd("ACTIVE");
            case "TERMINATED" -> {
                sub.setSubscriptionSttsCd("CANCELED");
                sub.setIsDeleted(true);
                sub.setEndDt(Optional.ofNullable(sub.getEndDt()).orElse(LocalDate.now()));
            }
            case "EXPIRED"   -> sub.setSubscriptionSttsCd("EXPIRED");
            default -> throw new IllegalArgumentException("Unsupported updateType: " + updateType);
        }
        sub.setUpdatedAt(OffsetDateTime.now());
    }

    // Enum overload (your enum package)
    private void transitionStatus(Subscription sub, SubscriptionUpdateType type) {
        if (type != null) {
            transitionStatus(sub, type.name());
        }
    }

    private String writeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialize\"}"; }
    }

    private String sha256(String s) {
        if (s == null) return null;
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String jsonMsg(String msg) {
        String safe = msg == null ? "" : msg.replace("\"", "'");
        return "{\"message\":\"" + safe + "\"}";
    }

    private void markAuditSuccess(Long id, String code, String desc, String detailsJson) {
        auditRepo.markProcessed(id, code, desc, detailsJson);
    }

    private void markAuditFailure(Long id, String code, String desc, String detailsJson) {
        auditRepo.markProcessed(id, code, desc, detailsJson);
    }

    private void emitOutbox(String aggregate, String id, String type, Map<String, ?> payload) {
        try {
            OutboxEvent ev = new OutboxEvent();
            ev.setAggregateType(aggregate);
            ev.setAggregateId(id);
            ev.setEventType(type);
            ev.setPayload(writeJson(payload));
            outboxRepo.save(ev);
        } catch (Exception e) {
            log.warn("Outbox emit failed: {} {} - {}", type, id, e.toString());
        }
    }

    // Success helpers with distinct names to avoid overload ambiguity
    private static ServiceResult<ReceiveSubscriptionNotificationRs> okNotification(ReceiveSubscriptionNotificationRs rs) {
        return new ServiceResult<>("I000000", "Successful Operation", null, rs);
    }
    private static ServiceResult<Void> okVoid() {
        return new ServiceResult<>("I000000", "Successful Operation", null, null);
    }
    private static <T> ServiceResult<T> err(String code, String desc, String details) {
        return new ServiceResult<>(code, desc, details, null);
    }
}
