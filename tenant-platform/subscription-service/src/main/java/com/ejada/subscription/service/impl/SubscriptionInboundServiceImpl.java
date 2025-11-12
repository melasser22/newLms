package com.ejada.subscription.service.impl;

import com.ejada.subscription.dto.ProductPropertyDto;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.ServiceResult;
import com.ejada.subscription.dto.SubscriptionAdditionalServiceDto;
import com.ejada.subscription.dto.SubscriptionFeatureDto;
import com.ejada.subscription.dto.SubscriptionInfoDto;
import com.ejada.subscription.dto.SubscriptionUpdateType;
import com.ejada.subscription.mapper.SubscriptionAdditionalServiceMapper;
import com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper;
import com.ejada.subscription.mapper.SubscriptionFeatureMapper;
import com.ejada.subscription.mapper.SubscriptionMapper;
import com.ejada.subscription.mapper.SubscriptionProductPropertyMapper;
import com.ejada.subscription.mapper.SubscriptionUpdateEventMapper;
import com.ejada.subscription.model.InboundNotificationAudit;
import com.ejada.subscription.model.OutboxEvent;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionAdditionalService;
import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import com.ejada.subscription.model.SubscriptionFeature;
import com.ejada.subscription.model.SubscriptionProductProperty;
import com.ejada.subscription.messaging.TenantOnboardingProducer;
import com.ejada.subscription.repository.InboundNotificationAuditRepository;
import com.ejada.subscription.repository.IdempotentRequestRepository;
import com.ejada.subscription.repository.OutboxEventRepository;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionProductPropertyRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.ejada.subscription.repository.SubscriptionUpdateEventRepository;
import com.ejada.subscription.security.JwtValidator;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    private final TenantOnboardingProducer tenantOnboardingProducer;

    // JSON
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private final ObjectMapper objectMapper;
    private final JwtValidator jwtValidator;

    private static final String EP_NOTIFICATION = "RECEIVE_NOTIFICATION";
    private static final String EP_UPDATE       = "RECEIVE_UPDATE";
    private static final String ERR_UNAUTHORIZED_CODE = "ESEC401";
    private static final String ERR_UNAUTHORIZED_DESC = "Unauthorized";
    private static final String INVALID_TOKEN_MESSAGE = "invalid or expired token";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ServiceResult<ReceiveSubscriptionNotificationRs> receiveSubscriptionNotification(
            final UUID rqUid,
            final String token,
            final ReceiveSubscriptionNotificationRq rq) {

        var existingAudit = auditRepo.findByRqUidAndEndpoint(rqUid, EP_NOTIFICATION).orElse(null);

        String normalizedToken = normalizeToken(token);

        if (!jwtValidator.isValid(normalizedToken)) {
            log.warn("Rejecting {} due to invalid subscription token", EP_NOTIFICATION);
            if (existingAudit != null) {
                if (!Boolean.TRUE.equals(existingAudit.getProcessed())) {
                    markAuditFailure(existingAudit.getInboundNotificationAuditId(), ERR_UNAUTHORIZED_CODE,
                            ERR_UNAUTHORIZED_DESC, jsonMsg(INVALID_TOKEN_MESSAGE));
                }
                String details = Optional.ofNullable(existingAudit.getStatusDtls())
                        .orElse(jsonMsg(INVALID_TOKEN_MESSAGE));
                return err(ERR_UNAUTHORIZED_CODE, ERR_UNAUTHORIZED_DESC, details);
            }
            return unauthorized(rqUid, normalizedToken, rq, EP_NOTIFICATION);
        }

        // 1) Idempotency shortcut (replay same response)
        if (existingAudit != null && Boolean.TRUE.equals(existingAudit.getProcessed())) {
            if (ERR_UNAUTHORIZED_CODE.equals(existingAudit.getStatusCode())) {
                String details = Optional.ofNullable(existingAudit.getStatusDtls())
                        .orElse(jsonMsg(INVALID_TOKEN_MESSAGE));
                return err(ERR_UNAUTHORIZED_CODE, ERR_UNAUTHORIZED_DESC, details);
            }
            var maybeSub = subscriptionRepo.findByExtSubscriptionIdAndExtCustomerId(
                    rq.subscriptionInfo().subscriptionId(), rq.subscriptionInfo().customerId());
            List<SubscriptionEnvironmentIdentifier> ids = maybeSub
                    .map(s -> envIdRepo.findBySubscriptionSubscriptionId(s.getSubscriptionId()))
                    .orElseGet(List::of);
            var rs = new ReceiveSubscriptionNotificationRs(Boolean.TRUE, envIdMapper.toDtoList(ids));
            return okNotification(rs);
        }

        // 2) Persist inbound audit row
        InboundNotificationAudit audit = Optional.ofNullable(existingAudit).orElseGet(InboundNotificationAudit::new);
        if (audit.getInboundNotificationAuditId() == null) {
            audit.setRqUid(rqUid);
            audit.setEndpoint(EP_NOTIFICATION);
        }
        audit.setTokenHash(sha256(normalizedToken));
        audit.setPayload(writeJson(rq));
        audit.setProcessed(Boolean.FALSE);
        audit.setProcessedAt(null);
        audit.setStatusCode(null);
        audit.setStatusDesc(null);
        audit.setStatusDtls(null);
        audit = auditRepo.save(audit);

        try {
            // 3) Upsert subscription
            SubscriptionInfoDto si = rq.subscriptionInfo();
            Subscription sub = subscriptionRepo
                    .findByExtSubscriptionIdAndExtCustomerId(si.subscriptionId(), si.customerId())
                    .orElse(null);

            boolean isNewSubscription;
            if (sub == null) {
                sub = subscriptionMapper.toEntity(si);
                isNewSubscription = true;
            } else {
                subscriptionMapper.update(sub, si);
                isNewSubscription = false;
            }

            if (sub.getEndDt() == null) {
                sub.setEndDt(Optional.ofNullable(si.endDt()).orElse(LocalDate.now()));
            }
            sub = subscriptionRepo.save(sub);

            // 4) Replace children from payload
            replaceFeatures(sub, si.subscriptionFeatureLst());
            replaceAdditionalServices(sub, si.subscriptionAdditionalServicesLst());
            replaceProductProperties(sub, rq.productProperties());

            if (isNewSubscription) {
                emitOnboardingEvents(sub, rq);
            }

            // 5) (Optional) environment identifiers (if provisioning already occurred)
            List<SubscriptionEnvironmentIdentifier> envIds =
                    envIdRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());

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

        var existingAudit = auditRepo.findByRqUidAndEndpoint(rqUid, EP_UPDATE).orElse(null);

        String normalizedToken = normalizeToken(token);

        if (!jwtValidator.isValid(normalizedToken)) {
            log.warn("Rejecting {} due to invalid subscription token", EP_UPDATE);
            if (existingAudit != null) {
                if (!Boolean.TRUE.equals(existingAudit.getProcessed())) {
                    markAuditFailure(existingAudit.getInboundNotificationAuditId(), ERR_UNAUTHORIZED_CODE,
                            ERR_UNAUTHORIZED_DESC, jsonMsg(INVALID_TOKEN_MESSAGE));
                }
                String details = Optional.ofNullable(existingAudit.getStatusDtls())
                        .orElse(jsonMsg(INVALID_TOKEN_MESSAGE));
                return err(ERR_UNAUTHORIZED_CODE, ERR_UNAUTHORIZED_DESC, details);
            }
            return unauthorized(rqUid, normalizedToken, rq, EP_UPDATE);
        }

        // 1) Idempotency by rqUID
        if (updateEventRepo.findByRqUid(rqUid).isPresent()) {
            return okVoid();
        }

        // 2) Audit row
        InboundNotificationAudit audit = Optional.ofNullable(existingAudit).orElseGet(InboundNotificationAudit::new);
        if (audit.getInboundNotificationAuditId() == null) {
            audit.setRqUid(rqUid);
            audit.setEndpoint(EP_UPDATE);
        }
        audit.setTokenHash(sha256(normalizedToken));
        audit.setPayload(writeJson(rq));
        audit.setProcessed(Boolean.FALSE);
        audit.setProcessedAt(null);
        audit.setStatusCode(null);
        audit.setStatusDesc(null);
        audit.setStatusDtls(null);
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

    private void replaceFeatures(final Subscription sub, final List<SubscriptionFeatureDto> dtos) {
        var existing = featureRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
        if (!existing.isEmpty()) {
            featureRepo.deleteAllInBatch(existing);
        }
        if (dtos != null && !dtos.isEmpty()) {
            var mapped = new ArrayList<SubscriptionFeature>(dtos.size());
            for (var d : dtos) {
                mapped.add(featureMapper.toEntity(d, sub));
            }
            featureRepo.saveAll(mapped);
        }
    }

    private void replaceAdditionalServices(final Subscription sub, final List<SubscriptionAdditionalServiceDto> dtos) {
        var existing = additionalServiceRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
        if (!existing.isEmpty()) {
            additionalServiceRepo.deleteAllInBatch(existing);
        }
        if (dtos != null && !dtos.isEmpty()) {
            var mapped = new ArrayList<SubscriptionAdditionalService>(dtos.size());
            for (var d : dtos) {
                mapped.add(additionalServiceMapper.toEntity(d, sub));
            }
            additionalServiceRepo.saveAll(mapped);
        }
    }

    private void replaceProductProperties(final Subscription sub, final List<ProductPropertyDto> dtos) {
        var existing = propertyRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
        if (!existing.isEmpty()) {
            propertyRepo.deleteAllInBatch(existing);
        }
        if (dtos != null && !dtos.isEmpty()) {
            var mapped = new ArrayList<SubscriptionProductProperty>(dtos.size());
            for (var d : dtos) {
                mapped.add(propertyMapper.toEntity(d, sub));
            }
            propertyRepo.saveAll(mapped);
        }
    }

    private void emitOnboardingEvents(
            final Subscription sub,
            final ReceiveSubscriptionNotificationRq rq) {

        SubscriptionInfoDto info = rq.subscriptionInfo();
        Map<String, Object> basePayload = baseOnboardingPayload(sub, info);

        Map<String, Object> tenantPayload = new LinkedHashMap<>(basePayload);
        tenantPayload.put("customerInfo", rq.customerInfo());
        tenantOnboardingProducer.publishTenantCreateRequested(sub, rq.customerInfo(), rq.adminUserInfo());
        emitOutbox("ONBOARDING", sub.getSubscriptionId().toString(), "TENANT_CREATE_REQUESTED", tenantPayload);

        Map<String, Object> catalogPayload = new LinkedHashMap<>(basePayload);
        catalogPayload.put("features", info.subscriptionFeatureLst());
        catalogPayload.put("additionalServices", info.subscriptionAdditionalServicesLst());
        catalogPayload.put("productProperties", rq.productProperties());
        emitOutbox("ONBOARDING", sub.getSubscriptionId().toString(), "CATALOG_SETUP_REQUESTED", catalogPayload);

        Map<String, Object> billingPayload = new LinkedHashMap<>(basePayload);
        billingPayload.put("subscriptionAmount", info.subscriptionAmount());
        billingPayload.put("totalBilledAmount", info.totalBilledAmount());
        billingPayload.put("totalPaidAmount", info.totalPaidAmount());
        billingPayload.put("startDt", info.startDt());
        billingPayload.put("endDt", info.endDt());
        billingPayload.put("createChannel", info.createChannel());
        billingPayload.put("unlimitedUsersFlag", info.unlimitedUsersFlag());
        billingPayload.put("usersLimit", info.usersLimit());
        billingPayload.put("usersLimitResetType", info.usersLimitResetType());
        billingPayload.put("unlimitedTransFlag", info.unlimitedTransFlag());
        billingPayload.put("transactionsLimit", info.transactionsLimit());
        billingPayload.put("transLimitResetType", info.transLimitResetType());
        billingPayload.put("balanceLimit", info.balanceLimit());
        billingPayload.put("balanceLimitResetType", info.balanceLimitResetType());
        emitOutbox("ONBOARDING", sub.getSubscriptionId().toString(), "BILLING_SETUP_REQUESTED", billingPayload);

        Map<String, Object> adminPayload = new LinkedHashMap<>(basePayload);
        adminPayload.put("adminUserInfo", rq.adminUserInfo());
        emitOutbox("ONBOARDING", sub.getSubscriptionId().toString(), "TENANT_ADMIN_CREATE_REQUESTED", adminPayload);
    }

    private Map<String, Object> baseOnboardingPayload(
            final Subscription sub,
            final SubscriptionInfoDto info) {

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subscriptionId", sub.getSubscriptionId());
        payload.put("extSubscriptionId", sub.getExtSubscriptionId());
        payload.put("extCustomerId", sub.getExtCustomerId());
        payload.put("productId", info.productId());
        payload.put("tierId", info.tierId());
        payload.put("tierNameEn", info.tierNameEn());
        payload.put("tierNameAr", info.tierNameAr());
        payload.put("environmentSizeCd", info.environmentSizeCd());
        payload.put("isAutoProvEnabled", info.isAutoProvEnabled());
        payload.put("prevSubscriptionId", info.prevSubscriptionId());
        payload.put("prevSubscriptionUpdateAction", info.prevSubscriptionUpdateAction());
        return payload;
    }

    // String-based version (kept for flexibility)
    private void transitionStatus(final Subscription sub, final String updateType) {
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
    private void transitionStatus(final Subscription sub, final SubscriptionUpdateType type) {
        if (type != null) {
            transitionStatus(sub, type.name());
        }
    }

    private String writeJson(final Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"serialize\"}";
        }
    }

    private String normalizeToken(final String rawToken) {
        if (rawToken == null) {
            return null;
        }

        String fallback = null;
        String[] parts = rawToken.split(",");
        for (String part : parts) {
            String candidate = part == null ? null : part.trim();
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (candidate.regionMatches(true, 0, "Bearer ", 0, 7)) {
                candidate = candidate.substring(7).trim();
            }
            if (candidate.isEmpty()) {
                continue;
            }
            if (candidate.indexOf('.') > 0) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private String sha256(final String s) {
        if (s == null) {
            return null;
        }
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String jsonMsg(final String msg) {
        String safe = msg == null ? "" : msg.replace("\"", "'");
        return "{\"message\":\"" + safe + "\"}";
    }

    private void markAuditSuccess(final Long id, final String code, final String desc, final String detailsJson) {
        auditRepo.markProcessed(id, code, desc, detailsJson);
    }

    private void markAuditFailure(final Long id, final String code, final String desc, final String detailsJson) {
        auditRepo.markProcessed(id, code, desc, detailsJson);
    }

    private void emitOutbox(final String aggregate, final String id, final String type, final Map<String, ?> payload) {
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
    private static ServiceResult<ReceiveSubscriptionNotificationRs> okNotification(final ReceiveSubscriptionNotificationRs rs) {
        return new ServiceResult<>("I000000", "Successful Operation", null, rs);
    }
    private static ServiceResult<Void> okVoid() {
        return new ServiceResult<>("I000000", "Successful Operation", null, null);
    }
    private static <T> ServiceResult<T> err(final String code, final String desc, final String details) {
        return new ServiceResult<>(code, desc, details, null);
    }

    private <T> ServiceResult<T> unauthorized(
            final UUID rqUid,
            final String token,
            final Object payload,
            final String endpoint) {

        InboundNotificationAudit audit = new InboundNotificationAudit();
        audit.setRqUid(rqUid);
        audit.setEndpoint(endpoint);
        audit.setTokenHash(sha256(token));
        audit.setPayload(writeJson(payload));
        audit = auditRepo.save(audit);

        String details = jsonMsg(INVALID_TOKEN_MESSAGE);
        markAuditFailure(audit.getInboundNotificationAuditId(), ERR_UNAUTHORIZED_CODE, ERR_UNAUTHORIZED_DESC, details);
        return err(ERR_UNAUTHORIZED_CODE, ERR_UNAUTHORIZED_DESC, details);
    }
}
