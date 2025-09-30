package com.ejada.catalog.controller;

import static com.ejada.common.http.BaseResponseEntityFactory.build;

import com.ejada.catalog.dto.TierFeatureCreateReq;
import com.ejada.catalog.dto.TierFeatureRes;
import com.ejada.catalog.dto.TierFeatureUpdateReq;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<BaseResponse<TierFeatureRes>> attach(@PathVariable @Min(1) final Integer tierId,
                                                               @Valid @RequestBody final TierFeatureCreateReq req) {
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
        BaseResponse<TierFeatureRes> response = service.attach(normalized);
        return build(response, HttpStatus.CREATED);
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
    public ResponseEntity<BaseResponse<TierFeatureRes>> update(@PathVariable @Min(1) final Integer tierId,
                                                               @PathVariable @Min(1) final Integer id,
                                                               @Valid @RequestBody final TierFeatureUpdateReq req) {
        // tierId is only for routing; service validates existence internally
        return build(service.update(id, req));
    }

    @CatalogAuthorized
    @Operation(summary = "List tier features", description = "Retrieves a paginated list of features attached to a tier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier features retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<TierFeatureRes>>> listByTier(@PathVariable @Min(1) final Integer tierId,
                                                                         @ParameterObject final Pageable pageable) {
        return build(service.listByTier(tierId, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Detach tier feature", description = "Soft deletes the association between a tier and a feature")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tier feature detached"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier feature not found")
    })
    public ResponseEntity<Void> detach(@PathVariable @Min(1) final Integer tierId,
                                       @PathVariable @Min(1) final Integer id) {
        service.detach(id);
        return ResponseEntity.noContent().build();
    }
}
