package com.ejada.setup.controller;

import static com.ejada.common.http.BaseResponseEntityFactory.build;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.CountryDto;
import com.ejada.setup.model.Country;
import com.ejada.setup.constants.ValidationConstants;
import com.ejada.setup.security.SetupAuthorized;
import com.ejada.setup.service.CountryService;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/setup/countries")
@Validated
@Tag(name = "Country Management", description = "APIs for managing countries")
public class CountryController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final CountryService countryService;
    
    public CountryController(final CountryService countryService) {
        this.countryService = countryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new country", description = "Creates a new country with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Country created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Country code already exists")
    })
    public ResponseEntity<BaseResponse<Country>> add(@Valid @RequestBody final CountryDto body) {
        BaseResponse<Country> response = countryService.add(body);
        return build(response);
    }

    @PutMapping("/{countryId}")
    @PreAuthorize("hasRole('ADMIN')")
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
            @PathVariable @Min(1) final Integer countryId,
            @Valid @RequestBody final CountryDto body) {
        BaseResponse<Country> response = countryService.update(countryId, body);
        return build(response);
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
            @PathVariable @Min(1) final Integer countryId) {
        BaseResponse<Country> response = countryService.get(countryId);
        return build(response);
    }

    @GetMapping
    //@RequireTenant
    @SetupAuthorized
    @Operation(summary = "List countries", description = "Retrieves a paginated list of countries with optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Countries retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<?>> list(
            @PageableDefault(size = ValidationConstants.PAGE_SIZE_DEFAULT) final Pageable pageable,
            @Parameter(description = "Search query for country names")
            @RequestParam(required = false) final String q,
            @Parameter(description = "Whether to retrieve all countries (ignores pagination)")
            @RequestParam(name = "unpaged", defaultValue = "false") final boolean unpaged) {
        Pageable effectivePageable = unpaged ? Pageable.unpaged() : pageable;
        BaseResponse<?> response = countryService.list(effectivePageable, q, unpaged);
        return build(response);
    }

    @GetMapping("/active")
    @SetupAuthorized
    @Operation(summary = "List active countries", description = "Retrieves all active countries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active countries retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<Country>>> listActive() {
        BaseResponse<List<Country>> response = countryService.listActive();
        return build(response);
    }
}
