package com.ejada.subscription.acl;

import static com.ejada.subscription.acl.MarketplaceCallbackEndpoints.NOTIFICATION;
import static com.ejada.subscription.acl.MarketplaceCallbackEndpoints.UPDATE;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.exception.ServiceResultException;
import com.ejada.common.marketplace.subscription.dto.ProductPropertyDto;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.common.marketplace.subscription.dto.SubscriptionAdditionalServiceDto;
import com.ejada.common.marketplace.subscription.dto.SubscriptionFeatureDto;
import com.ejada.common.marketplace.subscription.dto.SubscriptionInfoDto;
import com.ejada.common.marketplace.subscription.dto.SubscriptionUpdateType;
import com.ejada.subscription.acl.service.IdempotentRequestService;
import com.ejada.subscription.acl.service.NotificationAuditService;
import com.ejada.subscription.acl.service.NotificationReplayService;
import com.ejada.subscription.acl.service.SubscriptionOutboxService;
import com.ejada.subscription.kafka.SubscriptionApprovalPublisher;
import com.ejada.subscription.mapper.SubscriptionAdditionalServiceMapper;
import com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper;
import com.ejada.subscription.mapper.SubscriptionFeatureMapper;
import com.ejada.subscription.mapper.SubscriptionMapper;
import com.ejada.subscription.mapper.SubscriptionProductPropertyMapper;
import com.ejada.subscription.mapper.SubscriptionUpdateEventMapper;
import com.ejada.subscription.model.InboundNotificationAudit;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionAdditionalService;
import com.ejada.subscription.model.SubscriptionApprovalRequest;
import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import com.ejada.subscription.model.SubscriptionFeature;
import com.ejada.subscription.model.SubscriptionProductProperty;
import com.ejada.subscription.model.SubscriptionUpdateEvent;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionProductPropertyRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.ejada.subscription.repository.SubscriptionUpdateEventRepository;
import com.ejada.subscription.service.approval.ApprovalWorkflowService;
import com.ejada.subscription.service.approval.ApprovalWorkflowService.SubmissionResult;
import com.ejada.subscription.tenant.TenantLink;
import com.ejada.subscription.tenant.TenantLinkFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketplaceCallbackOrchestrator {

    private final SubscriptionRepository subscriptionRepo;
    private final SubscriptionFeatureRepository featureRepo;
    private final SubscriptionAdditionalServiceRepository additionalServiceRepo;
    private final SubscriptionProductPropertyRepository propertyRepo;
    private final SubscriptionEnvironmentIdentifierRepository envIdRepo;
    private final SubscriptionUpdateEventRepository updateEventRepo;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionFeatureMapper featureMapper;
    private final SubscriptionAdditionalServiceMapper additionalServiceMapper;
    private final SubscriptionProductPropertyMapper propertyMapper;
    private final SubscriptionEnvironmentIdentifierMapper envIdMapper;
    private final SubscriptionUpdateEventMapper updateEventMapper;
    private final SubscriptionApprovalPublisher approvalPublisher;
    private final TenantLinkFactory tenantLinkFactory;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages dependency scope")
    private final ApprovalWorkflowService approvalWorkflowService;
    private final NotificationReplayService notificationReplayService;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages dependency scope")
    private final NotificationAuditService notificationAuditService;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages dependency scope")
    private final SubscriptionOutboxService subscriptionOutboxService;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages dependency scope")
    private final IdempotentRequestService idempotentRequestService;

    @Transactional
    public ServiceResult<ReceiveSubscriptionNotificationRs> processNotification(
            final UUID rqUid, final String token, final ReceiveSubscriptionNotificationRq rq) {

        Objects.requireNonNull(rq, "request must not be null");

        var replay = notificationReplayService.replayNotificationIfProcessed(rqUid, rq);
        if (replay != null) {
            return replay;
        }

        InboundNotificationAudit audit =
                notificationAuditService.recordInboundAudit(rqUid, token, rq, NOTIFICATION);

        try {
            SubscriptionInfoDto si = rq.subscriptionInfo();
            UpsertResult upsert = upsertSubscription(si);
            Subscription sub = upsert.subscription();
            synchronizeSubscriptionChildren(sub, rq);

            SubmissionResult submissionResult =
                    approvalWorkflowService.submitForApproval(sub, rq, upsert.isNew());

            return switch (submissionResult.state()) {
                case AUTO_APPROVED -> {
                    TenantLink tenantLink = tenantLinkFactory.resolve(rq, sub);
                    boolean updated = updateTenantLinkIfMissing(sub, tenantLink);
                    if (updated) {
                        subscriptionRepo.save(sub);
                    }
                    approvalPublisher.publishApprovalDecision(
                            SubscriptionApprovalAction.APPROVED,
                            rqUid,
                            sub,
                            rq.customerInfo(),
                            rq.adminUserInfo(),
                            tenantLink,
                            submissionResult.autoApprovalRule());
                    List<SubscriptionEnvironmentIdentifier> envIds = fetchEnvironmentIdentifiers(sub);
                    ReceiveSubscriptionNotificationRs response =
                            new ReceiveSubscriptionNotificationRs(
                                    Boolean.TRUE, envIdMapper.toDtoList(envIds));
                    finalizeNotificationSuccess(
                            audit, rqUid, rq, sub, "I000000", "Subscription auto-approved");
                    yield okNotification(response);
                }
                case ALREADY_APPROVED -> {
                    List<SubscriptionEnvironmentIdentifier> envIds = fetchEnvironmentIdentifiers(sub);
                    ReceiveSubscriptionNotificationRs response =
                            new ReceiveSubscriptionNotificationRs(
                                    Boolean.TRUE, envIdMapper.toDtoList(envIds));
                    finalizeNotificationSuccess(
                            audit, rqUid, rq, sub, "I000000", "Subscription already approved");
                    yield okNotification(response);
                }
                case PENDING -> {
                    TenantLink tenantLink = tenantLinkFactory.resolve(rq, sub);
                    boolean updated = updateTenantLinkIfMissing(sub, tenantLink);
                    if (updated) {
                        subscriptionRepo.save(sub);
                    }
                    approvalPublisher.publishApprovalRequest(rqUid, rq, sub);
                    finalizeNotificationPending(
                            audit,
                            rqUid,
                            rq,
                            sub,
                            submissionResult.approvalRequest(),
                            true,
                            "Subscription submitted for approval");
                    ReceiveSubscriptionNotificationRs response =
                            new ReceiveSubscriptionNotificationRs(Boolean.TRUE, List.of());
                    yield ServiceResult.withSingleDetail(
                            rqUid != null ? rqUid.toString() : null,
                            "I000001",
                            "Subscription pending approval",
                            "Subscription requires manual approval",
                            response);
                }
                case ALREADY_PENDING -> {
                    finalizeNotificationPending(
                            audit,
                            rqUid,
                            rq,
                            sub,
                            submissionResult.approvalRequest(),
                            false,
                            "Subscription already submitted for approval");
                    ReceiveSubscriptionNotificationRs response =
                            new ReceiveSubscriptionNotificationRs(Boolean.TRUE, List.of());
                    yield ServiceResult.withSingleDetail(
                            rqUid != null ? rqUid.toString() : null,
                            "I000001",
                            "Subscription pending approval",
                            "Subscription already awaiting approval",
                            response);
                }
            };

        } catch (RuntimeException ex) {
            return handleNotificationFailure(audit, ex);
        }
    }

    @Transactional
    public ServiceResult<Void> processUpdate(
            final UUID rqUid, final String token, final ReceiveSubscriptionUpdateRq rq) {
        if (isDuplicateUpdate(rqUid)) {
            return okVoid();
        }

        InboundNotificationAudit audit =
                notificationAuditService.recordInboundAudit(rqUid, token, rq, UPDATE);

        try {
            SubscriptionUpdateEvent event = persistUpdateEvent(rq, rqUid);
            Subscription sub = findSubscriptionOrThrow(rq.subscriptionId());

            transitionStatus(sub, rq.subscriptionUpdateType());
            subscriptionRepo.save(sub);

            finalizeUpdateSuccess(audit, rqUid, rq, event, sub);

            return okVoid();

        } catch (EntityNotFoundException nf) {
            return handleUpdateFailure(audit, nf, "subscription not found for update");
        } catch (Exception ex) {
            return handleUpdateFailure(audit, ex, "processing failed");
        }
    }

    private UpsertResult upsertSubscription(final SubscriptionInfoDto info) {
        if (info == null) {
            throw new IllegalArgumentException("subscriptionInfo is required");
        }
        Optional<Subscription> existingOpt = subscriptionRepo
                .findByExtSubscriptionIdAndExtCustomerId(info.subscriptionId(), info.customerId());

        boolean isNew = existingOpt.isEmpty();
        Subscription sub = existingOpt
                .map(existing -> {
                    subscriptionMapper.update(existing, info);
                    return existing;
                })
                .orElseGet(() -> subscriptionMapper.toEntity(info));

        if (sub.getEndDt() == null) {
            sub.setEndDt(Optional.ofNullable(info.endDt()).orElse(LocalDate.now()));
        }
        Subscription saved = subscriptionRepo.save(sub);
        return new UpsertResult(saved, isNew);
    }

    private boolean updateTenantLinkIfMissing(final Subscription sub, final TenantLink link) {
        if (link == null) {
            return false;
        }
        boolean changed = false;
        if (link.tenantCode() != null && !link.tenantCode().equals(sub.getTenantCode())) {
            sub.setTenantCode(link.tenantCode());
            changed = true;
        }
        if (link.securityTenantId() != null
                && !link.securityTenantId().equals(sub.getSecurityTenantId())) {
            sub.setSecurityTenantId(link.securityTenantId());
            changed = true;
        }
        return changed;
    }

    private void synchronizeSubscriptionChildren(final Subscription sub, final ReceiveSubscriptionNotificationRq rq) {
        SubscriptionInfoDto info = rq.subscriptionInfo();
        replaceFeatures(sub, info.subscriptionFeatureLst());
        replaceAdditionalServices(sub, info.subscriptionAdditionalServicesLst());
        replaceProductProperties(sub, rq.productProperties());
    }

    private List<SubscriptionEnvironmentIdentifier> fetchEnvironmentIdentifiers(final Subscription sub) {
        return envIdRepo.findBySubscriptionSubscriptionId(sub.getSubscriptionId());
    }

    private void finalizeNotificationSuccess(
            final InboundNotificationAudit audit,
            final UUID rqUid,
            final ReceiveSubscriptionNotificationRq rq,
            final Subscription sub,
            final String statusCode,
            final String statusDescription) {
        Map<String, Object> payload = buildSubscriptionPayload(sub);
        subscriptionOutboxService.emit(
                "SUBSCRIPTION", sub.getSubscriptionId().toString(), "CREATED_OR_UPDATED", payload);
        idempotentRequestService.record(rqUid, NOTIFICATION, rq);
        notificationAuditService.markSuccess(
                audit.getInboundNotificationAuditId(), statusCode, statusDescription, null);
    }

    private void finalizeNotificationPending(
            final InboundNotificationAudit audit,
            final UUID rqUid,
            final ReceiveSubscriptionNotificationRq rq,
            final Subscription sub,
            final SubscriptionApprovalRequest approvalRequest,
            final boolean emitEvent,
            final String description) {
        if (emitEvent && approvalRequest != null) {
            Map<String, Object> payload = buildSubscriptionPayload(sub);
            payload.put("approvalRequestId", approvalRequest.getApprovalRequestId());
            if (approvalRequest.getRiskLevel() != null) {
                payload.put("riskLevel", approvalRequest.getRiskLevel());
            }
            if (approvalRequest.getPriority() != null) {
                payload.put("priority", approvalRequest.getPriority());
            }
            subscriptionOutboxService.emit(
                    "SUBSCRIPTION",
                    sub.getSubscriptionId().toString(),
                    "SUBSCRIPTION_APPROVAL_REQUESTED",
                    payload);
        }
        idempotentRequestService.record(rqUid, NOTIFICATION, rq);
        notificationAuditService.markSuccess(
                audit.getInboundNotificationAuditId(), "I000001", description, null);
    }

    private Map<String, Object> buildSubscriptionPayload(final Subscription sub) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("extSubscriptionId", sub.getExtSubscriptionId());
        payload.put("extCustomerId", sub.getExtCustomerId());
        return payload;
    }

    private ServiceResult<ReceiveSubscriptionNotificationRs> handleNotificationFailure(
            final InboundNotificationAudit audit, final Exception ex) {
        log.error("receiveSubscriptionNotification failed", ex);
        var failure = err("EINT000", "Unexpected Error", jsonMsg("processing failed"));
        notificationAuditService.markFailure(
                audit.getInboundNotificationAuditId(),
                "EINT000",
                "Unexpected Error",
                jsonMsg(ex.getMessage()));
        notificationAuditService.markRollbackOnlyIfActive();
        throw new ServiceResultException(failure, ex);
    }

    private boolean isDuplicateUpdate(final UUID rqUid) {
        return rqUid != null && updateEventRepo.findByRqUid(rqUid).isPresent();
    }

    private SubscriptionUpdateEvent persistUpdateEvent(final ReceiveSubscriptionUpdateRq rq, final UUID rqUid) {
        var event = updateEventMapper.toEvent(rq, rqUid);
        return updateEventRepo.save(event);
    }

    private Subscription findSubscriptionOrThrow(final Long subscriptionId) {
        return subscriptionRepo
                .findByExtSubscriptionId(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Unknown subscriptionId: " + subscriptionId));
    }

    private void finalizeUpdateSuccess(
            final InboundNotificationAudit audit,
            final UUID rqUid,
            final ReceiveSubscriptionUpdateRq rq,
            final SubscriptionUpdateEvent event,
            final Subscription sub) {
        event.setProcessed(true);
        event.setProcessedAt(OffsetDateTime.now());
        subscriptionOutboxService.emit(
                "SUBSCRIPTION",
                sub.getSubscriptionId().toString(),
                "STATUS_CHANGED",
                Map.of("newStatus", sub.getSubscriptionSttsCd()));
        idempotentRequestService.record(rqUid, UPDATE, rq);
        notificationAuditService.markSuccess(
                audit.getInboundNotificationAuditId(),
                "I000000",
                "Successful Operation",
                null);
    }

    private ServiceResult<Void> handleUpdateFailure(
            final InboundNotificationAudit audit, final Exception ex, final String message) {
        if (!(ex instanceof EntityNotFoundException)) {
            log.error("receiveSubscriptionUpdate failed", ex);
        }
        var failure = err("EINT000", "Unexpected Error", jsonMsg(message));
        notificationAuditService.markFailure(
                audit.getInboundNotificationAuditId(),
                "EINT000",
                "Unexpected Error",
                jsonMsg(ex.getMessage()));
        notificationAuditService.markRollbackOnlyIfActive();
        throw new ServiceResultException(failure, ex);
    }

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

    private void transitionStatus(final Subscription sub, final String updateType) {
        switch (updateType) {
            case "SUSPENDED" -> sub.setSubscriptionSttsCd("SUSPENDED");
            case "RESUMED" -> sub.setSubscriptionSttsCd("ACTIVE");
            case "TERMINATED" -> {
                sub.setSubscriptionSttsCd("CANCELED");
                sub.setIsDeleted(true);
                sub.setEndDt(Optional.ofNullable(sub.getEndDt()).orElse(LocalDate.now()));
            }
            case "EXPIRED" -> sub.setSubscriptionSttsCd("EXPIRED");
            default -> throw new IllegalArgumentException("Unsupported updateType: " + updateType);
        }
        sub.setUpdatedAt(OffsetDateTime.now());
    }

    private void transitionStatus(final Subscription sub, final SubscriptionUpdateType type) {
        if (type != null) {
            transitionStatus(sub, type.name());
        }
    }

    private String jsonMsg(final String msg) {
        String safe = msg == null ? "" : msg.replace("\"", "'");
        return "{\"message\":\"" + safe + "\"}";
    }

    private static ServiceResult<ReceiveSubscriptionNotificationRs> okNotification(
            final ReceiveSubscriptionNotificationRs rs) {
        return ServiceResult.ok(rs);
    }

    private static ServiceResult<Void> okVoid() {
        return ServiceResult.ok(null);
    }

    private static <T> ServiceResult<T> err(final String code, final String desc, final String details) {
        List<String> detailList =
                (details == null || details.isBlank()) ? List.of() : List.of(details);
        return ServiceResult.error(null, code, desc, detailList);
    }

    private record UpsertResult(Subscription subscription, boolean isNew) {
    }
}
