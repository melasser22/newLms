package com.ejada.tenant.controller;

import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.dto.TenantRes;
import com.ejada.tenant.dto.TenantUpdateReq;
import com.ejada.tenant.security.TenantAuthorized;
import com.ejada.tenant.service.TenantService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tenant Management", description = "APIs for managing tenants")
public final class TenantController {

    private final TenantService service;

    @PostMapping
    @TenantAuthorized
    @Operation(summary = "Create a new tenant", description = "Creates a new tenant with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant created successfully",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Tenant already exists")
    })
    public ResponseEntity<BaseResponse<TenantRes>> create(@Valid @RequestBody final TenantCreateReq req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @TenantAuthorized
    @Operation(summary = "Update an existing tenant", description = "Updates the tenant with the specified ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<BaseResponse<TenantRes>> update(@PathVariable @Min(1) final Integer id,
                                                          @Valid @RequestBody final TenantUpdateReq req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @TenantAuthorized
    @Operation(summary = "Get tenant by ID", description = "Retrieves the tenant with the specified ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<TenantRes>> get(@PathVariable @Min(1) final Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    @TenantAuthorized
    @Operation(summary = "List tenants", description = "Retrieves a paginated list of tenants, optionally filtered by name and active flag")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenants retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<TenantRes>>> list(
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) final Boolean active,
            @ParameterObject final Pageable pageable) {
        return ResponseEntity.ok(service.list(name, active, pageable));
    }

    @TenantAuthorized
    @Operation(summary = "Delete tenant", description = "Soft deletes the tenant with the specified ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tenant deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) final Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}