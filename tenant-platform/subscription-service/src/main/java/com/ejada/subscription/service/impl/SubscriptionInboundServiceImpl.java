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
import com.ejada.subscription.exception.ServiceResultException;
import com.ejada.subscription.mapper.SubscriptionAdditionalServiceMapper;
import com.ejada.subscription.kafka.SubscriptionApprovalPublisher;
import com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper;
import com.ejada.subscription.mapper.SubscriptionFeatureMapper;
import com.ejada.subscription.mapper.SubscriptionMapper;
import com.ejada.subscription.mapper.SubscriptionProductPropertyMapper;
import com.ejada.subscription.mapper.SubscriptionUpdateEventMapper;
import com.ejada.subscription.model.InboundNotificationAudit;
import com.ejada.subscription.model.IdempotentRequest;
import com.ejada.subscription.model.OutboxEvent;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionAdditionalService;
import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import com.ejada.subscription.model.SubscriptionFeature;
import com.ejada.subscription.model.SubscriptionProductProperty;
import com.ejada.subscription.repository.InboundNotificationAuditRepository;
import com.ejada.subscription.repository.IdempotentRequestRepository;
import com.ejada.subscription.repository.OutboxEventRepository;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionProductPropertyRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.ejada.subscription.repository.SubscriptionUpdateEventRepository;
import com.ejada.subscription.service.SubscriptionInboundService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
    private final PlatformTransactionManager transactionManager;
    private final SubscriptionApprovalPublisher approvalPublisher;

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
                    .map(s -> envIdRepo.findBySubscriptionSubscriptionId(s.getSubscriptionId()))
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
        audit = executeInNewTransaction(() -> auditRepo.save(audit), "persist inbound notification audit");

        try {
            // 3) Upsert subscription
            SubscriptionInfoDto si = rq.subscriptionInfo();
            Subscription sub = subscriptionRepo
                    .findByExtSubscriptionIdAndExtCustomerId(si.subscriptionId(), si.customerId())
                    .orElse(null);

            boolean isNewSubscription = (sub == null);

            if (sub == null) {
                sub = subscriptionMapper.toEntity(si);
            } else {
                subscriptionMapper.update(sub, si);
            }

            if (sub.getEndDt() == null) {
                sub.setEndDt(Optional.ofNullable(si.endDt()).orElse(LocalDate.now()));
            }
            sub = subscriptionRepo.save(sub);

            // 4) Replace children from payload
            replaceFeatures(sub, si.subscriptionFeatureLst());
            replaceAdditionalServices(sub, si.subscriptionAdditionalServicesLst());
            replaceProductProperties(sub, rq.productProperties());

            // 5) (Optional) environment identifiers (if provisioning already occurred)
            List<SubscriptionEnvironmentIdentifier> envIds =
                    envIdRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());

            // 6) Mark success and emit outbox
            emitOutbox("SUBSCRIPTION", sub.getSubscriptionId().toString(), "CREATED_OR_UPDATED",
                    Map.of("extSubscriptionId", sub.getExtSubscriptionId(), "extCustomerId", sub.getExtCustomerId()));

            if (isNewSubscription) {
                approvalPublisher.publishApprovalRequest(rqUid, rq, sub);
            }

            recordIdempotentRequest(rqUid, EP_NOTIFICATION, rq);
            markAuditSuccess(audit.getInboundNotificationAuditId(), "I000000", "Successful Operation", null);

            var rs = new ReceiveSubscriptionNotificationRs(Boolean.TRUE, envIdMapper.toDtoList(envIds));
            return okNotification(rs);

        } catch (Exception ex) {
            log.error("receiveSubscriptionNotification failed", ex);
            var failure = err("EINT000", "Unexpected Error", jsonMsg("processing failed"));
            markAuditFailure(audit.getInboundNotificationAuditId(), "EINT000", "Unexpected Error",
                    jsonMsg(ex.getMessage()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new ServiceResultException(failure, ex);
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
        audit = executeInNewTransaction(() -> auditRepo.save(audit), "persist inbound notification audit");

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

            emitOutbox("SUBSCRIPTION", sub.getSubscriptionId().toString(), "STATUS_CHANGED",
                    Map.of("newStatus", sub.getSubscriptionSttsCd()));

            recordIdempotentRequest(rqUid, EP_UPDATE, rq);
            markAuditSuccess(audit.getInboundNotificationAuditId(), "I000000", "Successful Operation", null);

            return okVoid();

        } catch (EntityNotFoundException nf) {
            var failure = err("EINT000", "Unexpected Error", jsonMsg("subscription not found for update"));
            markAuditFailure(audit.getInboundNotificationAuditId(), "EINT000", "Unexpected Error",
                    jsonMsg(nf.getMessage()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new ServiceResultException(failure, nf);

        } catch (Exception ex) {
            log.error("receiveSubscriptionUpdate failed", ex);
            var failure = err("EINT000", "Unexpected Error", jsonMsg("processing failed"));
            markAuditFailure(audit.getInboundNotificationAuditId(), "EINT000", "Unexpected Error",
                    jsonMsg(ex.getMessage()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new ServiceResultException(failure, ex);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void replaceFeatures(final Subscription sub, final List<SubscriptionFeatureDto> dtos) {
        var existing = featureRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
        if (dtos == null || dtos.isEmpty()) {
            if (!existing.isEmpty()) {
                featureRepo.deleteAll(existing);
            }
            return;
        }

        var existingByCode = new java.util.HashMap<String, SubscriptionFeature>(existing.size());
        for (SubscriptionFeature feature : existing) {
            existingByCode.put(feature.getFeatureCd(), feature);
        }

        var toCreate = new ArrayList<SubscriptionFeature>();
        var toUpdate = new ArrayList<SubscriptionFeature>();

        for (SubscriptionFeatureDto dto : dtos) {
            SubscriptionFeature entity = existingByCode.remove(dto.featureCd());
            if (entity == null) {
                toCreate.add(featureMapper.toEntity(dto, sub));
            } else {
                entity.setFeatureCount(dto.featureCount());
                entity.setUpdatedAt(OffsetDateTime.now());
                toUpdate.add(entity);
            }
        }

        if (!existingByCode.isEmpty()) {
            featureRepo.deleteAll(existingByCode.values());
        }
        if (!toUpdate.isEmpty()) {
            featureRepo.saveAll(toUpdate);
        }
        if (!toCreate.isEmpty()) {
            featureRepo.saveAll(toCreate);
        }
    }

    private void replaceAdditionalServices(final Subscription sub, final List<SubscriptionAdditionalServiceDto> dtos) {
        var existing = additionalServiceRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
        if (dtos == null || dtos.isEmpty()) {
            if (!existing.isEmpty()) {
                additionalServiceRepo.deleteAll(existing);
            }
            return;
        }

        var existingById = new java.util.HashMap<Long, SubscriptionAdditionalService>(existing.size());
        for (SubscriptionAdditionalService svc : existing) {
            existingById.put(svc.getProductAdditionalServiceId(), svc);
        }

        var toCreate = new ArrayList<SubscriptionAdditionalService>();
        var toUpdate = new ArrayList<SubscriptionAdditionalService>();

        for (SubscriptionAdditionalServiceDto dto : dtos) {
            SubscriptionAdditionalService entity = existingById.remove(dto.productAdditionalServiceId());
            if (entity == null) {
                toCreate.add(additionalServiceMapper.toEntity(dto, sub));
            } else {
                entity.setServiceCd(dto.serviceCd());
                entity.setServiceNameEn(dto.serviceNameEn());
                entity.setServiceNameAr(dto.serviceNameAr());
                entity.setServiceDescEn(dto.serviceDescEn());
                entity.setServiceDescAr(dto.serviceDescAr());
                entity.setServicePrice(dto.servicePrice());
                entity.setTotalAmount(dto.totalAmount());
                entity.setCurrency(dto.currency());
                entity.setIsCountable(Boolean.TRUE.equals(dto.isCountable()));
                entity.setRequestedCount(dto.requestedCount());
                entity.setPaymentTypeCd(dto.paymentTypeCd());
                entity.setUpdatedAt(OffsetDateTime.now());
                toUpdate.add(entity);
            }
        }

        if (!existingById.isEmpty()) {
            additionalServiceRepo.deleteAll(existingById.values());
        }
        if (!toUpdate.isEmpty()) {
            additionalServiceRepo.saveAll(toUpdate);
        }
        if (!toCreate.isEmpty()) {
            additionalServiceRepo.saveAll(toCreate);
        }
    }

    private void replaceProductProperties(final Subscription sub, final List<ProductPropertyDto> dtos) {
        var existing = propertyRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
        if (dtos == null || dtos.isEmpty()) {
            if (!existing.isEmpty()) {
                propertyRepo.deleteAll(existing);
            }
            return;
        }

        var existingByCode = new java.util.HashMap<String, SubscriptionProductProperty>(existing.size());
        for (SubscriptionProductProperty prop : existing) {
            existingByCode.put(prop.getPropertyCd(), prop);
        }

        var toCreate = new ArrayList<SubscriptionProductProperty>();
        var toUpdate = new ArrayList<SubscriptionProductProperty>();

        for (ProductPropertyDto dto : dtos) {
            SubscriptionProductProperty entity = existingByCode.remove(dto.propertyCd());
            if (entity == null) {
                toCreate.add(propertyMapper.toEntity(dto, sub));
            } else {
                entity.setPropertyValue(dto.propertyValue());
                toUpdate.add(entity);
            }
        }

        if (!existingByCode.isEmpty()) {
            propertyRepo.deleteAll(existingByCode.values());
        }
        if (!toUpdate.isEmpty()) {
            propertyRepo.saveAll(toUpdate);
        }
        if (!toCreate.isEmpty()) {
            propertyRepo.saveAll(toCreate);
        }
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
        runInNewTransaction(() -> auditRepo.markProcessed(id, code, desc, detailsJson),
                "mark inbound audit success");
    }

    private void markAuditFailure(final Long id, final String code, final String desc, final String detailsJson) {
        runInNewTransaction(() -> auditRepo.markProcessed(id, code, desc, detailsJson),
                "mark inbound audit failure");
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
            throw new IllegalStateException("Unable to persist outbox event", e);
        }
    }

    private void recordIdempotentRequest(final UUID rqUid, final String endpoint, final Object payload) {
        if (rqUid == null) {
            return;
        }
        runInNewTransaction(() -> {
            if (idemRepo.existsByIdempotencyKey(rqUid)) {
                return;
            }
            IdempotentRequest request = new IdempotentRequest();
            request.setIdempotencyKey(rqUid);
            request.setEndpoint(endpoint);
            request.setRequestHash(sha256(writeJson(payload)));
            try {
                idemRepo.save(request);
            } catch (Exception ex) {
                log.warn("Failed to persist idempotent request {} for endpoint {}", rqUid, endpoint, ex);
            }
        }, "persist idempotent request");
    }

    private void runInNewTransaction(final Runnable task, final String description) {
        try {
            executeInNewTransaction(
                    () -> {
                        task.run();
                        return null;
                    },
                    description);
        } catch (RuntimeException ignored) {
            // already logged inside executeInNewTransaction
        }
    }

    private <T> T executeInNewTransaction(final Supplier<T> supplier, final String description) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return template.execute(status -> supplier.get());
        } catch (RuntimeException txEx) {
            log.error("{}", description, txEx);
            throw txEx;
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
}
