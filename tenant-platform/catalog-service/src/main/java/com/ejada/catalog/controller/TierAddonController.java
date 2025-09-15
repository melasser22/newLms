package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.TierAddonService;
import com.ejada.common.dto.BaseResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@RequestMapping("/api/v1/catalog/tiers/{tierId}/addons")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tier Addon Management", description = "APIs for managing addons allowed for tiers")
public class TierAddonController {
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final TierAddonService service;

    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Allow addon for tier", description = "Bundles an addon for a given tier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier addon allowed",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Tier addon already exists")
    })
    public ResponseEntity<BaseResponse<TierAddonRes>> allow(@PathVariable @Min(1) Integer tierId,
                                                            @Valid @RequestBody TierAddonCreateReq req) {
        TierAddonCreateReq normalized = new TierAddonCreateReq(
            tierId,
            req.addonId(),
            req.included(),
            req.sortOrder(),
            req.basePrice(),
            req.currency()
        );
        return ResponseEntity.ok(service.allow(normalized));
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update tier addon", description = "Updates an existing tier addon")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier addon updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier addon not found")
    })
    public ResponseEntity<BaseResponse<TierAddonRes>> update(@PathVariable @Min(1) Integer tierId,
                                                              @PathVariable @Min(1) Integer id,
                                                              @Valid @RequestBody TierAddonUpdateReq req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping
    @CatalogAuthorized
    @Operation(summary = "List tier addons", description = "Retrieves a paginated list of addons allowed for a tier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier addons retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    public ResponseEntity<BaseResponse<Page<TierAddonRes>>> listByTier(@PathVariable @Min(1) Integer tierId,
                                                                       @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.listByTier(tierId, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Remove tier addon", description = "Soft deletes the tier addon association")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tier addon removed"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier addon not found")
    })
    public ResponseEntity<Void> remove(@PathVariable @Min(1) Integer tierId, @PathVariable @Min(1) Integer id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }
}
