package com.ejada.subscription.controller;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.web.ServiceResultResponses;
import com.ejada.subscription.dto.admin.AdminApproveSubscriptionRequest;
import com.ejada.subscription.dto.admin.AdminApproveSubscriptionResponse;
import com.ejada.subscription.service.approval.AdminApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing administrator actions for subscription approvals.
 */
@RestController
@RequestMapping(value = "/api/v1/admin/approvals", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AdminApprovalController {

    private final AdminApprovalService approvalService;

    @PostMapping(value = "/{approvalRequestId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_EJADA_OFFICER')")
    public ResponseEntity<ServiceResult<AdminApproveSubscriptionResponse>> approve(
            @PathVariable final Long approvalRequestId,
            @Valid @RequestBody final AdminApproveSubscriptionRequest body) {

        ServiceResult<AdminApproveSubscriptionResponse> result =
                approvalService.approve(approvalRequestId, body);
        return ServiceResultResponses.respond(result);
    }
}
