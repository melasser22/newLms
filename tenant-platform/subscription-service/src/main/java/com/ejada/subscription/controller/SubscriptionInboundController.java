package com.ejada.subscription.controller;

import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.ServiceResult;
import com.ejada.subscription.service.SubscriptionInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class SubscriptionInboundController {

    private final SubscriptionInboundService service;

    @PostMapping(
        value = "/receiveSubscriptionNotification",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ServiceResult<ReceiveSubscriptionNotificationRs>> receiveSubscriptionNotification(
            @RequestHeader("rqUID") final UUID rqUid,
            @RequestHeader(value = "token", required = false) final java.util.List<String> tokenHeaders,
            @Valid @RequestBody final ReceiveSubscriptionNotificationRq body) {

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                service.receiveSubscriptionNotification(rqUid, collapseTokenHeaders(tokenHeaders), body);
        return ResponseEntity.ok(result);
    }

    @PostMapping(
        value = "/receiveSubscriptionUpdate",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ServiceResult<Void>> receiveSubscriptionUpdate(
            @RequestHeader("rqUID") final UUID rqUid,
            @RequestHeader(value = "token", required = false) final java.util.List<String> tokenHeaders,
            @Valid @RequestBody final ReceiveSubscriptionUpdateRq body) {

        ServiceResult<Void> result =
                service.receiveSubscriptionUpdate(rqUid, collapseTokenHeaders(tokenHeaders), body);
        return ResponseEntity.ok(result);
    }

    private String collapseTokenHeaders(final java.util.List<String> tokenHeaders) {
        if (tokenHeaders == null || tokenHeaders.isEmpty()) {
            return null;
        }
        return tokenHeaders.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }
}
