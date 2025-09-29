package com.ejada.billing.service.impl;

import com.ejada.billing.dto.ProductConsumptionStts;
import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.dto.ServiceResult;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.billing.exception.ServiceResultException;
import com.ejada.billing.mapper.ConsumptionResponseMapper;
import com.ejada.billing.mapper.UsageCounterMapper;
import com.ejada.billing.mapper.UsageEventMapper;
import com.ejada.billing.model.UsageCounter;
import com.ejada.billing.repository.UsageCounterRepository;
import com.ejada.billing.repository.UsageEventRepository;
import com.ejada.billing.service.ConsumptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ConsumptionServiceImpl implements ConsumptionService {

    private final UsageCounterRepository counterRepo;
    private final UsageEventRepository eventRepo;
    private final UsageCounterMapper counterMapper;
    private final ConsumptionResponseMapper responseMapper;
    private final UsageEventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNewTx;

    public ConsumptionServiceImpl(final UsageCounterRepository counterRepo,
                                  final UsageEventRepository eventRepo,
                                  final UsageCounterMapper counterMapper,
                                  final ConsumptionResponseMapper responseMapper,
                                  final UsageEventMapper eventMapper,
                                  final ObjectMapper objectMapper,
                                  final PlatformTransactionManager transactionManager) {
        this.counterRepo = counterRepo;
        this.eventRepo = eventRepo;
        this.counterMapper = counterMapper;
        this.responseMapper = responseMapper;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper.copy();
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceResult<TrackProductConsumptionRs> trackProductConsumption(final UUID rqUid,
                                                                            final String token,
                                                                            final TrackProductConsumptionRq rq) {
        try {
            List<ProductSubscriptionStts> subscriptions = buildSubscriptionStatuses(rq);
            TrackProductConsumptionRs body = responseMapper.toResponse(rq.productId(), subscriptions);

            persistSuccessAudit(rqUid, token, rq);

            return ServiceResult.ok(rqUid.toString(), body);

        } catch (RuntimeException ex) {
            String debugId = Long.toString(System.nanoTime());
            ServiceResult<TrackProductConsumptionRs> failure =
                    ServiceResult.error(rqUid.toString(), debugId, List.of("Unexpected Error"));
            recordFailureAudit(rqUid, token, rq, ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new ServiceResultException(failure, ex);
        }
    }

    private List<ProductSubscriptionStts> buildSubscriptionStatuses(final TrackProductConsumptionRq rq) {
        List<ProductSubscriptionStts> subs = new ArrayList<>();
        if (rq == null || rq.activeSubscriptions() == null) {
            return subs;
        }
        for (var subscription : rq.activeSubscriptions()) {
            subs.add(buildSubscriptionStatus(subscription));
        }
        return subs;
    }

    private ProductSubscriptionStts buildSubscriptionStatus(final com.ejada.billing.dto.ProductSubscription sub) {
        List<ProductConsumptionStts> perTypes = new ArrayList<>();
        if (sub.productConsumption() != null) {
            for (var consumption : sub.productConsumption()) {
                perTypes.add(resolveConsumptionSnapshot(sub, consumption));
            }
        }
        return responseMapper.toSubscriptionStts(sub.customerId(), sub.subscriptionId(), perTypes);
    }

    private ProductConsumptionStts resolveConsumptionSnapshot(
            final com.ejada.billing.dto.ProductSubscription sub,
            final com.ejada.billing.dto.ProductConsumption consumption) {
        String typeCode = consumption.consumptionTypCd().name();
        UsageCounter counter = counterRepo
                .findByExtSubscriptionIdAndConsumptionTypCd(sub.subscriptionId(), typeCode)
                .orElseGet(() -> UsageCounter.builder()
                        .extSubscriptionId(sub.subscriptionId())
                        .extCustomerId(sub.customerId())
                        .consumptionTypCd(typeCode)
                        .currentConsumption(0L)
                        .currentConsumedAmt(BigDecimal.ZERO)
                        .build());
        UsageCounter snapshot = saveCounterSnapshot(counter, sub.subscriptionId(), typeCode);
        return counterMapper.toDto(snapshot);
    }

    private void persistSuccessAudit(final UUID rqUid,
                                     final String token,
                                     final TrackProductConsumptionRq rq) {
        eventRepo.save(eventMapper.build(
                rqUid,
                sha256(token),
                toJson(rq),
                rq != null ? rq.productId() : null,
                "I000000",
                "Successful Operation",
                null
        ));
    }

    private UsageCounter saveCounterSnapshot(final UsageCounter counter,
                                             final Long extSubscriptionId,
                                             final String typeCode) {
        try {
            return counterRepo.save(counter);
        } catch (DataIntegrityViolationException duplicate) {
            return counterRepo
                    .findByExtSubscriptionIdAndConsumptionTypCd(extSubscriptionId, typeCode)
                    .orElseThrow(() -> duplicate);
        }
    }

    private void recordFailureAudit(final UUID rqUid,
                                    final String token,
                                    final TrackProductConsumptionRq rq,
                                    final RuntimeException ex) {
        try {
            requiresNewTx.executeWithoutResult(status ->
                    eventRepo.save(eventMapper.build(
                            rqUid,
                            sha256(token),
                            safeJson(rq),
                            rq != null ? rq.productId() : null,
                            "EINT000",
                            "Unexpected Error",
                            toJson(List.of(ex.getClass().getSimpleName() + ": " + ex.getMessage()))
                    )));
        } catch (RuntimeException auditEx) {
            log.warn("Failed to persist error audit for trackProductConsumption {}", rqUid, auditEx);
        }
    }

    private String toJson(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String safeJson(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String sha256(final String s) {
        if (s == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
