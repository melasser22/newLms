package com.ejada.catalog.controller;

import static com.ejada.common.http.BaseResponseEntityFactory.build;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.AddonFeatureService;
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
@RequestMapping("/api/v1/catalog/addons/{addonId}/features")
@RequiredArgsConstructor
@Validated
@Tag(name = "Addon Feature Management", description = "APIs for managing addon features")
public class AddonFeatureController {

    private final AddonFeatureService service;

    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Attach feature to addon", description = "Creates an addon-feature link")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Addon feature attached",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Addon feature already exists")
    })
    public ResponseEntity<BaseResponse<AddonFeatureRes>> attach(@PathVariable @Min(1) Integer addonId,
                                                                @Valid @RequestBody AddonFeatureCreateReq req) {
        AddonFeatureCreateReq normalized = new AddonFeatureCreateReq(
            addonId,
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
        BaseResponse<AddonFeatureRes> response = service.attach(normalized);
        return build(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update addon feature", description = "Updates an existing addon feature")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Addon feature updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon feature not found")
    })
    public ResponseEntity<BaseResponse<AddonFeatureRes>> update(@PathVariable @Min(1) Integer addonId,
                                                                @PathVariable @Min(1) Integer id,
                                                                @Valid @RequestBody AddonFeatureUpdateReq req) {
        return build(service.update(id, req));
    }

    @GetMapping
    @CatalogAuthorized
    @Operation(summary = "List addon features", description = "Retrieves a paginated list of features attached to an addon")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Addon features retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon not found")
    })
    public ResponseEntity<BaseResponse<Page<AddonFeatureRes>>> listByAddon(@PathVariable @Min(1) Integer addonId,
                                                                           @ParameterObject Pageable pageable) {
        return build(service.listByAddon(addonId, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Detach addon feature", description = "Soft deletes the addon feature link")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Addon feature detached"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon feature not found")
    })
    public ResponseEntity<Void> detach(@PathVariable @Min(1) Integer addonId, @PathVariable @Min(1) Integer id) {
        service.detach(id);
        return ResponseEntity.noContent().build();
    }
}
