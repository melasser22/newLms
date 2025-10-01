package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.setup.dto.CityDto;
import com.ejada.setup.constants.ValidationConstants;
import com.ejada.setup.service.CityService;
import com.ejada.starter_security.authorization.PlatformServiceAuthorized;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/setup/cities")
@Validated
@Tag(name = "City Management", description = "APIs for managing cities")
public class CityController extends BaseResponseController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final CityService cityService;

    public CityController(final CityService cityService) {
        this.cityService = cityService;
    }

    @PostMapping
    @PlatformServiceAuthorized
    @Operation(summary = "Create a new city", description = "Creates a new city with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "City created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "City already exists")
    })
    public ResponseEntity<BaseResponse<CityDto>> add(@Valid @RequestBody final CityDto body) {
        return respond(() -> cityService.add(body));
    }

    @PutMapping("/{cityId}")
    @PlatformServiceAuthorized
    @Operation(summary = "Update an existing city", description = "Updates the city with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "City updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "City not found")
    })
    public ResponseEntity<BaseResponse<CityDto>> update(
            @Parameter(description = "ID of the city to update", required = true)
            @PathVariable @Min(1) final Integer cityId,
            @Valid @RequestBody final CityDto body) {
        return respond(() -> cityService.update(cityId, body));
    }

    @GetMapping("/{cityId}")
    @PlatformServiceAuthorized
    @Operation(summary = "Get city by ID", description = "Retrieves a city by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "City found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "City not found")
    })
    public ResponseEntity<BaseResponse<CityDto>> get(
            @Parameter(description = "ID of the city to retrieve", required = true)
            @PathVariable @Min(1) final Integer cityId) {
        return respond(() -> cityService.get(cityId));
    }

    @GetMapping
    @PlatformServiceAuthorized
    @Operation(summary = "List cities", description = "Retrieves a paginated list of cities with optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cities retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<Page<CityDto>>> list(
            @PageableDefault(size = ValidationConstants.PAGE_SIZE_DEFAULT) final Pageable pageable,
            @Parameter(description = "Search query for city names")
            @RequestParam(required = false) final String q,
            @Parameter(description = "Whether to retrieve all cities (ignores pagination)")
            @RequestParam(name = "unpaged", defaultValue = "false") final boolean unpaged) {
        Pageable effectivePageable = unpaged ? Pageable.unpaged() : pageable;
        return respond(() -> cityService.list(effectivePageable, q, unpaged));
    }

    @GetMapping("/active")
    @PlatformServiceAuthorized
    @Operation(summary = "List active cities by country", description = "Retrieves all active cities for the given country")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active cities retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<CityDto>>> listActive(
            @Parameter(description = "Country ID to filter cities", required = true)
            @RequestParam @Min(1) final Integer countryId) {
        return respond(() -> cityService.listActiveByCountry(countryId));
    }
}
