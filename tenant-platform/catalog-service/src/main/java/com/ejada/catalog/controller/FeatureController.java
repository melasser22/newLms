package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.FeatureService;
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
@RequestMapping("/api/v1/catalog/features")
@RequiredArgsConstructor
@Validated
@Tag(name = "Feature Management", description = "APIs for managing features")
public class FeatureController {

    private final FeatureService service;

    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Create a new feature", description = "Creates a new feature with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feature created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "featureKey already exists")
    })
    public ResponseEntity<BaseResponse<FeatureRes>> create(@Valid @RequestBody FeatureCreateReq req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update an existing feature", description = "Updates the feature with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feature updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Feature not found")
    })
    public ResponseEntity<BaseResponse<FeatureRes>> update(@PathVariable @Min(1) Integer id,
                                                           @Valid @RequestBody FeatureUpdateReq req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Get feature by ID", description = "Retrieves the feature with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feature retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Feature not found")
    })
    public ResponseEntity<BaseResponse<FeatureRes>> get(@PathVariable @Min(1) Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    @CatalogAuthorized
    @Operation(summary = "List features", description = "Retrieves a paginated list of features, optionally filtered by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Features retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<Page<FeatureRes>>> list(@RequestParam(required = false) String category,
                                                               @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.list(category, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Delete feature", description = "Soft deletes the feature with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Feature deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Feature not found")
    })
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
