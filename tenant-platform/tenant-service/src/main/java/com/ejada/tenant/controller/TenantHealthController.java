package com.ejada.tenant.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.tenant.dto.TenantHealthScoreRes;
import com.ejada.tenant.security.TenantAuthorized;
import com.ejada.tenant.service.TenantHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tenant Health", description = "APIs for tenant health scoring")
public class TenantHealthController extends BaseResponseController {

    private final TenantHealthService tenantHealthService;

    @GetMapping("/{id}/health-score")
    @TenantAuthorized
    @Operation(summary = "Get tenant health score", description = "Retrieves the latest health score for a tenant")
    @ApiResponse(responseCode = "200", description = "Tenant health score retrieved",
            content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    public ResponseEntity<BaseResponse<TenantHealthScoreRes>> getHealthScore(@PathVariable("id") @Min(1) final Integer tenantId) {
        return respond(() -> tenantHealthService.getHealthScore(tenantId));
    }
}
