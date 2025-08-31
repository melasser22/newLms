package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Lookup;
import com.lms.setup.security.SetupAuthorized;
import com.lms.setup.service.LookupService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup/lookups")
@Validated
@Tag(name = "Lookup Management", description = "APIs for managing lookup values")
public class LookupController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
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
    public ResponseEntity<BaseResponse<Lookup>> add(@Valid @RequestBody Lookup body) {
        return ResponseEntity.ok(lookupService.add(body));
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
    public ResponseEntity<BaseResponse<Lookup>> update(
            @Parameter(description = "ID of the lookup to update", required = true)
            @PathVariable @Min(1) Integer lookupItemId,
            @Valid @RequestBody Lookup body) {
        return ResponseEntity.ok(lookupService.update(lookupItemId, body));
    }

    @GetMapping
    @SetupAuthorized
    @Operation(summary = "Get all lookups", description = "Retrieves all lookup values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookups retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<Lookup>>> getAllLookups() {
        return ResponseEntity.ok(lookupService.getAllLookups());
    }

    @GetMapping("/group/{groupCode}")
    @SetupAuthorized
    @Operation(summary = "Get lookups by group", description = "Retrieves all lookups of a specific group")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookups retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<Lookup>>> getByGroup(
            @Parameter(description = "Group code of lookups to retrieve", required = true)
            @PathVariable @NotBlank String groupCode) {
        return ResponseEntity.ok(lookupService.getByGroup(groupCode));
    }

    @GetMapping("/all")
    @PreAuthorize("@roleChecker.hasRole(authentication, T(com.shared.starter_security.Role).EJADA_OFFICER, T(com.shared.starter_security.Role).TENANT_ADMIN, T(com.shared.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "Get all lookups (alternative endpoint)", description = "Retrieves all lookup values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lookups retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<Lookup>>> getAll() {
        return ResponseEntity.ok(lookupService.getAll());
    }
}
