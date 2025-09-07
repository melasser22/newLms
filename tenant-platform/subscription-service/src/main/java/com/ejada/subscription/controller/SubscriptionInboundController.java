package com.ejada.subscription.controller;

import com.ejada.subscription.dto.*;
import com.ejada.subscription.service.SubscriptionInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("rqUID") UUID rqUid,
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody ReceiveSubscriptionNotificationRq body) {

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                service.receiveSubscriptionNotification(rqUid, token, body);
        return ResponseEntity.ok(result);
    }

    @PostMapping(
        value = "/receiveSubscriptionUpdate",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ServiceResult<Void>> receiveSubscriptionUpdate(
            @RequestHeader("rqUID") UUID rqUid,
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody ReceiveSubscriptionUpdateRq body) {

        ServiceResult<Void> result = service.receiveSubscriptionUpdate(rqUid, token, body);
        return ResponseEntity.ok(result);
    }
}
