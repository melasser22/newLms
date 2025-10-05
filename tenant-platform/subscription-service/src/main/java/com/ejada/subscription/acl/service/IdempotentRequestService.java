package com.ejada.subscription.acl.service;

import com.ejada.common.marketplace.token.TokenHashing;
import com.ejada.subscription.model.IdempotentRequest;
import com.ejada.subscription.repository.IdempotentRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentRequestService {

    private final IdempotentRequestRepository idempotentRequestRepository;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring manages dependency scope")
    private final ObjectMapper objectMapper;
    private final NewTransactionExecutor newTransactionExecutor;

    public void record(final java.util.UUID rqUid, final String endpoint, final Object payload) {
        if (rqUid == null) {
            return;
        }
        newTransactionExecutor.run(
                () -> {
                    if (idempotentRequestRepository.existsByIdempotencyKey(rqUid)) {
                        return;
                    }
                    IdempotentRequest request = new IdempotentRequest();
                    request.setIdempotencyKey(rqUid);
                    request.setEndpoint(endpoint);
                    request.setRequestHash(TokenHashing.sha256(writeJson(payload)));
                    try {
                        idempotentRequestRepository.save(request);
                    } catch (Exception ex) {
                        log.warn(
                                "Failed to persist idempotent request {} for endpoint {}",
                                rqUid,
                                endpoint,
                                ex);
                    }
                },
                "persist idempotent request");
    }

    private String writeJson(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"error\":\"serialize\"}";
        }
    }
}
