package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.TierFeatureService;
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
@RequestMapping("/api/v1/catalog/tiers/{tierId}/features")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tier Feature Management", description = "APIs for managing features attached to tiers")
public class TierFeatureController {

    private final TierFeatureService service;

    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Attach feature to tier", description = "Creates an association between a tier and a feature")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier feature attached",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Tier feature already exists")
    })
    public ResponseEntity<BaseResponse<TierFeatureRes>> attach(@PathVariable @Min(1) Integer tierId,
                                                               @Valid @RequestBody TierFeatureCreateReq req) {
        // ensure path id wins if body is absent/mismatched
        TierFeatureCreateReq normalized = new TierFeatureCreateReq(
            tierId,
            req.featureId(),
            req.enabled(),
            req.enforcement(),
            req.softLimit(),
            req.hardLimit(),
            req.limitWindow(),
            req.measureUnit(),
            req.resetCron(),
            req.overageEnabled(),
            req.overageUnitPrice(),
            req.overageCurrency(),
            req.meta()
        );
        return ResponseEntity.ok(service.attach(normalized));
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update tier feature", description = "Updates an existing tier feature association")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier feature updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier feature not found")
    })
    public ResponseEntity<BaseResponse<TierFeatureRes>> update(@PathVariable @Min(1) Integer tierId,
                                                               @PathVariable @Min(1) Integer id,
                                                               @Valid @RequestBody TierFeatureUpdateReq req) {
        // tierId is only for routing; service validates existence internally
        return ResponseEntity.ok(service.update(id, req));
    }

    @CatalogAuthorized
    @Operation(summary = "List tier features", description = "Retrieves a paginated list of features attached to a tier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier features retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<TierFeatureRes>>> listByTier(@PathVariable @Min(1) Integer tierId,
                                                                         @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.listByTier(tierId, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Detach tier feature", description = "Soft deletes the association between a tier and a feature")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tier feature detached"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier feature not found")
    })
    public ResponseEntity<Void> detach(@PathVariable @Min(1) Integer tierId, @PathVariable @Min(1) Integer id) {
        service.detach(id);
        return ResponseEntity.noContent().build();
    }
}
