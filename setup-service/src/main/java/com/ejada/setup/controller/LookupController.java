package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.ApiStatusMapper;
import com.ejada.setup.dto.LookupCreateRequest;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.dto.LookupUpdateRequest;
import com.ejada.setup.security.SetupAuthorized;
import com.ejada.setup.service.LookupService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/setup/lookups")
@Validated
@Tag(name = "Lookup Management", description = "APIs for managing lookup values")
public class LookupController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final LookupService lookupService;

    public LookupController(final LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @PostMapping
    @SetupAuthorized
    @Operation(summary = "Create a new lookup", description = "Creates a new lookup value with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookup created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Lookup already exists")
    })
    public ResponseEntity<BaseResponse<LookupResponse>> add(
            @Valid @RequestBody final LookupCreateRequest body) {
        BaseResponse<LookupResponse> response = lookupService.add(body);
        return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
    }

    @PutMapping("/{lookupItemId}")
    @SetupAuthorized
    @Operation(summary = "Update an existing lookup", description = "Updates the lookup with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookup updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Lookup not found")
    })
    public ResponseEntity<BaseResponse<LookupResponse>> update(
            @Parameter(description = "ID of the lookup to update", required = true)
            @PathVariable @Min(1) final Integer lookupItemId,
            @Valid @RequestBody final LookupUpdateRequest body) {
        BaseResponse<LookupResponse> response = lookupService.update(lookupItemId, body);
        return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
    }

    @GetMapping
    @SetupAuthorized
    @Operation(summary = "Get all lookups", description = "Retrieves all lookup values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookups retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<LookupResponse>>> getAllLookups() {
        BaseResponse<List<LookupResponse>> response = lookupService.getAllLookups();
        return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
    }

    @GetMapping("/group/{groupCode}")
    @SetupAuthorized
    @Operation(summary = "Get lookups by group", description = "Retrieves all lookups of a specific group")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookups retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<LookupResponse>>> getByGroup(
            @Parameter(description = "Group code of lookups to retrieve", required = true)
            @PathVariable @NotBlank final String groupCode) {
        BaseResponse<List<LookupResponse>> response = lookupService.getByGroup(groupCode);
        return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
    }

    @GetMapping("/all")
    @PreAuthorize(
            "@roleChecker.hasRole(authentication, T(com.ejada.starter_security.Role).EJADA_OFFICER, "
                    + "T(com.ejada.starter_security.Role).TENANT_ADMIN, "
                    + "T(com.ejada.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "Get all lookups (alternative endpoint)", description = "Retrieves all lookup values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookups retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<LookupResponse>>> getAll() {
        BaseResponse<List<LookupResponse>> response = lookupService.getAll();
        return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
    }
}
