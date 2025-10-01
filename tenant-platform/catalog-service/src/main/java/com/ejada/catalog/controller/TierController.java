package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.security.CatalogAuthorized;
import com.ejada.catalog.service.TierService;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
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
@RequestMapping("/api/v1/catalog/tiers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tier Management", description = "APIs for managing tiers")
public class TierController extends BaseResponseController {

    private final TierService service;

    @PostMapping
    @CatalogAuthorized
    @Operation(summary = "Create a new tier", description = "Creates a new tier with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Tier already exists")
    })
    public ResponseEntity<BaseResponse<TierRes>> create(@Valid @RequestBody TierCreateReq req) {
        return respond(() -> service.create(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Update an existing tier", description = "Updates the tier with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    public ResponseEntity<BaseResponse<TierRes>> update(@PathVariable @Min(1) Integer id,
                                                        @Valid @RequestBody TierUpdateReq req) {
        return respond(() -> service.update(id, req));
    }

    @CatalogAuthorized
    @Operation(summary = "Get tier by ID", description = "Retrieves the tier with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<TierRes>> get(@PathVariable @Min(1) Integer id) {
        return respond(() -> service.get(id));
    }

    @CatalogAuthorized
    @Operation(summary = "List tiers", description = "Retrieves a paginated list of tiers, optionally filtered by active flag")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tiers retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<TierRes>>> list(@RequestParam(required = false) Boolean active,
                                                            @ParameterObject Pageable pageable) {
        return respond(() -> service.list(active, pageable));
    }

    @DeleteMapping("/{id}")
    @CatalogAuthorized
    @Operation(summary = "Delete tier", description = "Soft deletes the tier with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tier deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
