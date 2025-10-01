package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.AddonService;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/addons")
@RequiredArgsConstructor
@Validated
@Tag(name = "Addon Management", description = "APIs for managing addons")
@Slf4j
public class AddonController extends BaseResponseController {

    private final AddonService service;

    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Create a new addon", description = "Creates a new addon with the provided details")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Addon created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Addon already exists")
    })
    public ResponseEntity<BaseResponse<AddonRes>> create(@Valid @RequestBody AddonCreateReq req) {
        log.info("Request to create addon: {}", req);
        return respond(() -> service.create(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update an existing addon", description = "Updates the addon with the specified ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Addon updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon not found")
    })
    public ResponseEntity<BaseResponse<AddonRes>> update(@PathVariable @Min(1) Integer id,
                                                         @Valid @RequestBody AddonUpdateReq req) {
        log.info("Updating addon {} with {}", id, req);
        return respond(() -> service.update(id, req));
    }

    @GetMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Get addon by ID", description = "Retrieves the addon with the specified ID")
    public ResponseEntity<BaseResponse<AddonRes>> get(@PathVariable @Min(1) Integer id) {
        log.debug("Fetching addon {}", id);
        return respond(() -> service.get(id));
    }

    @GetMapping
    @CatalogAuthorized
    @Operation(summary = "List addons", description = "Retrieves a paginated list of addons, optionally filtered by category")
    public ResponseEntity<BaseResponse<Page<AddonRes>>> list(@RequestParam(required = false) String category,
                                                             @ParameterObject Pageable pageable) {
        log.debug("Listing addons for category={} page={}", category, pageable);
        return respond(() -> service.list(category, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Delete addon", description = "Soft deletes the addon with the specified ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Addon deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon not found")
    })
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Integer id) {
        log.warn("Soft deleting addon {}", id);
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
