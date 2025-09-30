package com.ejada.subscription.controller;

import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.ServiceResult;
import com.ejada.subscription.exception.ServiceResultException;
import com.ejada.subscription.service.SubscriptionInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @RequestHeader(value = "token", required = false) final String token,
            @Valid @RequestBody final ReceiveSubscriptionNotificationRq body) {

        try {
            ServiceResult<ReceiveSubscriptionNotificationRs> result =
                    service.receiveSubscriptionNotification(rqUid, token, body);
            return respond(result);
        } catch (ServiceResultException ex) {
            return respond(ex.getResult());
        }
    }

    @PostMapping(
        value = "/receiveSubscriptionUpdate",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ServiceResult<Void>> receiveSubscriptionUpdate(
            @RequestHeader("rqUID") final UUID rqUid,
            @RequestHeader(value = "token", required = false) final String token,
            @Valid @RequestBody final ReceiveSubscriptionUpdateRq body) {

        try {
            ServiceResult<Void> result = service.receiveSubscriptionUpdate(rqUid, token, body);
            return respond(result);
        } catch (ServiceResultException ex) {
            return respond(ex.getResult());
        }
    }

    private <T> ResponseEntity<ServiceResult<T>> respond(final ServiceResult<T> result) {
        if (result == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        HttpStatus status = Boolean.TRUE.equals(result.success())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(result);
    }
}
