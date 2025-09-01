package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Country;
import com.lms.setup.dto.CountryDto;
import com.lms.setup.security.SetupAuthorized;
import com.lms.setup.service.CountryService;
import com.shared.starter_core.tenant.RequireTenant;

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
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup/countries")
@Validated
@Tag(name = "Country Management", description = "APIs for managing countries")
public class CountryController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final CountryService countryService;
    
    public CountryController(CountryService countryService) { 
        this.countryService = countryService; 
    }

    @PostMapping
    @SetupAuthorized
    @Operation(summary = "Create a new country", description = "Creates a new country with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Country created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Country code already exists")
    })
    public ResponseEntity<BaseResponse<Country>> add(@Valid @RequestBody CountryDto body) {
        return ResponseEntity.ok(countryService.add(body));
    }

    @PutMapping("/{countryId}")
    @SetupAuthorized
    @Operation(summary = "Update an existing country", description = "Updates the country with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Country updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Country not found"),
        @ApiResponse(responseCode = "409", description = "Country code already exists")
    })
    public ResponseEntity<BaseResponse<Country>> update(
            @Parameter(description = "ID of the country to update", required = true)
            @PathVariable @Min(1) Integer countryId,
            @Valid @RequestBody CountryDto body) {
        return ResponseEntity.ok(countryService.update(countryId, body));
    }

    @GetMapping("/{countryId}")
    @SetupAuthorized
    @Operation(summary = "Get country by ID", description = "Retrieves a country by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Country found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Country not found")
    })
    public ResponseEntity<BaseResponse<Country>> get(
            @Parameter(description = "ID of the country to retrieve", required = true)
            @PathVariable @Min(1) Integer countryId) {
        return ResponseEntity.ok(countryService.get(countryId));
    }

    @GetMapping
    @RequireTenant
    @SetupAuthorized
    @Operation(summary = "List countries", description = "Retrieves a paginated list of countries with optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Countries retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<?> list(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Search query for country names")
            @RequestParam(required = false) String q,
            @Parameter(description = "Whether to retrieve all countries (ignores pagination)")
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged) {
        Pageable effectivePageable = unpaged ? Pageable.unpaged() : pageable;
        return ResponseEntity.ok(countryService.list(effectivePageable, q, unpaged));
    }

    @GetMapping("/active")
    @SetupAuthorized
    @Operation(summary = "List active countries", description = "Retrieves all active countries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active countries retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<Country>>> listActive() {
        return ResponseEntity.ok(countryService.listActive());
    }
}
