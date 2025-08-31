package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.dto.CityDto;
import com.lms.setup.service.CityService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setup/cities")
@Validated
@Tag(name = "City Management", description = "APIs for managing cities")
public class CityController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @PostMapping
    @PreAuthorize("@roleChecker.hasRole(authentication, T(com.shared.starter_security.Role).EJADA_OFFICER, T(com.shared.starter_security.Role).TENANT_ADMIN, T(com.shared.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "Create a new city", description = "Creates a new city with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "City created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "City already exists")
    })
    public ResponseEntity<BaseResponse<CityDto>> add(@Valid @RequestBody CityDto body) {
        return ResponseEntity.ok(cityService.add(body));
    }

    @PutMapping("/{cityId}")
    @PreAuthorize("@roleChecker.hasRole(authentication, T(com.shared.starter_security.Role).EJADA_OFFICER, T(com.shared.starter_security.Role).TENANT_ADMIN, T(com.shared.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "Update an existing city", description = "Updates the city with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "City updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "City not found")
    })
    public ResponseEntity<BaseResponse<CityDto>> update(
            @Parameter(description = "ID of the city to update", required = true)
            @PathVariable @Min(1) Integer cityId,
            @Valid @RequestBody CityDto body) {
        return ResponseEntity.ok(cityService.update(cityId, body));
    }

    @GetMapping("/{cityId}")
    @PreAuthorize("@roleChecker.hasRole(authentication, T(com.shared.starter_security.Role).EJADA_OFFICER, T(com.shared.starter_security.Role).TENANT_ADMIN, T(com.shared.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "Get city by ID", description = "Retrieves a city by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "City found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "City not found")
    })
    public ResponseEntity<BaseResponse<CityDto>> get(
            @Parameter(description = "ID of the city to retrieve", required = true)
            @PathVariable @Min(1) Integer cityId) {
        return ResponseEntity.ok(cityService.get(cityId));
    }

    @GetMapping
    @PreAuthorize("@roleChecker.hasRole(authentication, T(com.shared.starter_security.Role).EJADA_OFFICER, T(com.shared.starter_security.Role).TENANT_ADMIN, T(com.shared.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "List cities", description = "Retrieves a paginated list of cities with optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cities retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<?> list(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Search query for city names")
            @RequestParam(required = false) String q,
            @Parameter(description = "Whether to return all cities (ignores pagination)")
            @RequestParam(required = false) boolean all) {
        return ResponseEntity.ok(cityService.list(pageable, q, all));
    }

    @GetMapping("/active")
    @PreAuthorize("@roleChecker.hasRole(authentication, T(com.shared.starter_security.Role).EJADA_OFFICER, T(com.shared.starter_security.Role).TENANT_ADMIN, T(com.shared.starter_security.Role).TENANT_OFFICER)")
    @Operation(summary = "List active cities by country", description = "Retrieves all active cities for the given country")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active cities retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<?> listActive(
            @Parameter(description = "Country ID to filter cities", required = true)
            @RequestParam @Min(1) Integer countryId) {
        return ResponseEntity.ok(cityService.listActiveByCountry(countryId));
    }
}
