package com.ejada.billing.service.impl;

import com.ejada.billing.dto.ProductConsumptionStts;
import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.dto.ServiceResult;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.billing.mapper.ConsumptionResponseMapper;
import com.ejada.billing.mapper.UsageCounterMapper;
import com.ejada.billing.mapper.UsageEventMapper;
import com.ejada.billing.model.UsageCounter;
import com.ejada.billing.repository.UsageCounterRepository;
import com.ejada.billing.repository.UsageEventRepository;
import com.ejada.billing.service.ConsumptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public final class ConsumptionServiceImpl implements ConsumptionService {

    private final UsageCounterRepository counterRepo;
    private final UsageEventRepository eventRepo;
    private final UsageCounterMapper counterMapper;
    private final ConsumptionResponseMapper responseMapper;
    private final UsageEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public ConsumptionServiceImpl(final UsageCounterRepository counterRepo,
                                  final UsageEventRepository eventRepo,
                                  final UsageCounterMapper counterMapper,
                                  final ConsumptionResponseMapper responseMapper,
                                  final UsageEventMapper eventMapper,
                                  final ObjectMapper objectMapper) {
        this.counterRepo = counterRepo;
        this.eventRepo = eventRepo;
        this.counterMapper = counterMapper;
        this.responseMapper = responseMapper;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper.copy();
    }

    @Override
    @Transactional
    public ServiceResult<TrackProductConsumptionRs> trackProductConsumption(final UUID rqUid,
                                                                            final String token,
                                                                            final TrackProductConsumptionRq rq) {
        try {
            // Build response per subscription
            List<ProductSubscriptionStts> subs = new ArrayList<>();
            if (rq.activeSubscriptions() != null) {
                for (var sub : rq.activeSubscriptions()) {
                    List<ProductConsumptionStts> perTypes = new ArrayList<>();

                    if (sub.productConsumption() != null && !sub.productConsumption().isEmpty()) {
                        for (var pc : sub.productConsumption()) {
                            String typ = pc.consumptionTypCd().name(); // TRANSACTION|USER|BALANCE

                            // upsert snapshot rows if missing (no mutation/increment per swagger)
                            UsageCounter c = counterRepo
                                    .findByExtSubscriptionIdAndConsumptionTypCd(sub.subscriptionId(), typ)
                                    .orElseGet(() -> UsageCounter.builder()
                                            .extSubscriptionId(sub.subscriptionId())
                                            .extCustomerId(sub.customerId())
                                            .consumptionTypCd(typ)
                                            .currentConsumption(0L)
                                            .currentConsumedAmt(BigDecimal.ZERO)
                                            .build());
                            c = counterRepo.save(c);

                            // map entity snapshot -> DTO respecting enum rules
                            perTypes.add(counterMapper.toDto(c));
                        }
                    }

                    subs.add(responseMapper.toSubscriptionStts(sub.customerId(), sub.subscriptionId(), perTypes));
                }
            }

            TrackProductConsumptionRs body = responseMapper.toResponse(rq.productId(), subs);

            // persist immutable audit row
            eventRepo.save(eventMapper.build(
                    rqUid,
                    sha256(token),
                    toJson(rq),
                    rq.productId(),
                    "I000000",
                    "Successful Operation",
                    null
            ));

            return ServiceResult.ok(rqUid.toString(), body);

        } catch (RuntimeException ex) {
            String debugId = Long.toString(System.nanoTime());
            // best-effort audit with error
            try {
                eventRepo.save(eventMapper.build(
                        rqUid,
                        sha256(token),
                        safeJson(rq),
                        rq != null ? rq.productId() : null,
                        "EINT000",
                        "Unexpected Error",
                        toJson(List.of(ex.getClass().getSimpleName() + ": " + ex.getMessage()))
                ));
            } catch (RuntimeException ignored) { }
            return ServiceResult.error(rqUid.toString(), debugId, List.of("Unexpected Error"));
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
