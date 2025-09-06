package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.AddonService;
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
@RequestMapping("/api/v1/catalog/addons")
@RequiredArgsConstructor
@Validated
@Tag(name = "Addon Management", description = "APIs for managing addon")

public class AddonController {

    private final AddonService service;
    
    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Create a new addon", description = "Creates a new addon with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "addon created successfully",
                content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "addon already exists")
        })
    public ResponseEntity<BaseResponse<AddonRes>> create(@Valid @RequestBody AddonCreateReq req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update an existing addon", description = "Updates the addon with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "addon updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "addon not found")
    })
    public ResponseEntity<BaseResponse<AddonRes>> update(@PathVariable @Min(1) Integer id, @Valid @RequestBody AddonUpdateReq req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @CatalogAuthorized
    @Operation(summary = "Get addon by ID", description = "Retrieves the addon with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Addon retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AddonRes>> get(@PathVariable @Min(1) Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    @CatalogAuthorized
    @Operation(summary = "List addons", description = "Retrieves a paginated list of addons, optionally filtered by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Addons retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AddonRes>>> list(@RequestParam(required = false) String category,
                                                             @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.list(category, pageable));
    }

    @CatalogAuthorized
    @Operation(summary = "Delete addon", description = "Soft deletes the addon with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Addon deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Addon not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
