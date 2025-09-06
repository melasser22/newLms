package com.ejada.tenant.controller;

import com.ejada.tenant.dto.*;
import com.ejada.tenant.security.TenantAuthorized;
import com.ejada.tenant.service.TenantIntegrationKeyService;
import com.ejada.common.dto.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/keys")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tenant Integration Keys", description = "APIs for managing tenant integration keys")
public class TenantIntegrationKeyController {

	private final TenantIntegrationKeyService service;

	@PostMapping
	@TenantAuthorized
	@Operation(summary = "Create a new integration key", description = "Creates a new integration key for a tenant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Integration key created successfully", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "409", description = "Key already exists for tenant") })
	public ResponseEntity<BaseResponse<TenantIntegrationKeyRes>> create(
			@Valid @RequestBody TenantIntegrationKeyCreateReq req) {
		return ResponseEntity.ok(service.create(req));
	}

	@PutMapping("/{id}")
	@TenantAuthorized
	@Operation(summary = "Update an integration key", description = "Updates the integration key with the specified ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Integration key updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Integration key not found") })
	public ResponseEntity<BaseResponse<TenantIntegrationKeyRes>> update(@PathVariable("id") @Min(1) Long tikId,
			@Valid @RequestBody TenantIntegrationKeyUpdateReq req) {
		return ResponseEntity.ok(service.update(tikId, req));
	}

	@TenantAuthorized
	@Operation(summary = "Get integration key by ID", description = "Retrieves the integration key with the specified ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Integration key retrieved successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Integration key not found") })
	@GetMapping("/{id}")
	public ResponseEntity<BaseResponse<TenantIntegrationKeyRes>> get(@PathVariable("id") @Min(1) Long tikId) {
		return ResponseEntity.ok(service.get(tikId));
	}

	@TenantAuthorized
	@Operation(summary = "List integration keys by tenant", description = "Retrieves a paginated list of integration keys for a tenant")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Integration keys retrieved successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Tenant not found") })
	@GetMapping("/by-tenant/{tenantId}")
	public ResponseEntity<BaseResponse<Page<TenantIntegrationKeyRes>>> listByTenant(
			@PathVariable @Min(1) Integer tenantId, @ParameterObject Pageable pageable) {
		return ResponseEntity.ok(service.listByTenant(tenantId, pageable));
	}

	@TenantAuthorized
	@Operation(summary = "Revoke integration key", description = "Soft deletes and revokes the integration key with the specified ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Integration key revoked successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Integration key not found") })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> revoke(@PathVariable("id") @Min(1) Long tikId) {
		service.revoke(tikId);
		return ResponseEntity.noContent().build();
	}
}