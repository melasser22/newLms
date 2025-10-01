package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.setup.dto.SystemParameterRequest;
import com.ejada.setup.dto.SystemParameterResponse;
import com.ejada.setup.service.SystemParameterService;
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
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/setup/systemParameters")
@Validated
@Tag(name = "System Parameter Management", description = "APIs for managing system parameters")
public class SystemParameterController extends BaseResponseController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final SystemParameterService systemParameterService;

    public SystemParameterController(final SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    @PostMapping
    @PlatformServiceAuthorized
    @Operation(summary = "Create a new system parameter", description = "Creates a new system parameter with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "System parameter already exists")
    })

    public ResponseEntity<BaseResponse<SystemParameterResponse>> add(@Valid @RequestBody final SystemParameterRequest body) {
        return respond(() -> systemParameterService.add(body), org.springframework.http.HttpStatus.CREATED);
    }

    @PutMapping("/{paramId}")
    @PlatformServiceAuthorized
    @Operation(summary = "Update an existing system parameter", description = "Updates the system parameter with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "System parameter not found")
    })
    public ResponseEntity<BaseResponse<SystemParameterResponse>> update(
            @Parameter(description = "ID of the system parameter to update", required = true)
            @PathVariable @Min(1) final Integer paramId,
            @Valid @RequestBody final SystemParameterRequest body) {
        return respond(() -> systemParameterService.update(paramId, body));

    }

    @GetMapping("/{paramId}")
    @PlatformServiceAuthorized
    @Operation(summary = "Get system parameter by ID", description = "Retrieves a system parameter by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "System parameter not found")
    })
    public ResponseEntity<BaseResponse<SystemParameterResponse>> get(
            @Parameter(description = "ID of the system parameter to retrieve", required = true)
            @PathVariable @Min(1) final Integer paramId) {
        return respond(() -> systemParameterService.get(paramId));
    }

    @GetMapping
    @PlatformServiceAuthorized
    @Operation(summary = "List system parameters", description = "Retrieves a paginated list of system parameters with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameters retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<Page<SystemParameterResponse>>> list(
            @PageableDefault(size = DEFAULT_PAGE_SIZE) final Pageable pageable,
            @Parameter(description = "Group filter for parameters")
            @RequestParam(required = false) final String group,
            @Parameter(description = "Whether to return only active parameters")
            @RequestParam(required = false) final Boolean onlyActive,
            @Parameter(description = "Whether to retrieve all parameters (ignores pagination)")
            @RequestParam(name = "unpaged", defaultValue = "false") final boolean unpaged) {
        final Pageable effectivePageable = unpaged ? Pageable.unpaged() : pageable;
        return respond(() -> systemParameterService.list(effectivePageable, group, onlyActive));
    }

    @GetMapping("/by-key/{paramKey}")
    @PlatformServiceAuthorized
    @Operation(summary = "Get system parameter by key", description = "Retrieves a system parameter by its key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "System parameter not found")
    })
    public ResponseEntity<BaseResponse<SystemParameterResponse>> getByKey(
            @Parameter(description = "Key of the parameter to retrieve", required = true)
            @PathVariable @NotBlank final String paramKey) {
        return respond(() -> systemParameterService.getByKey(paramKey));

    }

    @PostMapping("/by-keys")
    @PlatformServiceAuthorized
    @Operation(summary = "Get system parameters by keys", description = "Retrieves multiple system parameters by their keys")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameters retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<SystemParameterResponse>>> getByKeys(
            @Parameter(description = "List of parameter keys to retrieve", required = true)
            @RequestBody final List<String> keys) {
        return respond(() -> systemParameterService.getByKeys(keys));
    }
}
