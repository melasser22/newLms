package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.SystemParameter;
import com.ejada.setup.security.SetupAuthorized;
import com.ejada.setup.service.SystemParameterService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup/systemParameters")
@Validated
@Tag(name = "System Parameter Management", description = "APIs for managing system parameters")
public class SystemParameterController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final SystemParameterService systemParameterService;

    public SystemParameterController(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    @PostMapping
    @SetupAuthorized
    @Operation(summary = "Create a new system parameter", description = "Creates a new system parameter with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "System parameter already exists")
    })
    public ResponseEntity<BaseResponse<SystemParameter>> add(@Valid @RequestBody SystemParameter body) {
        return ResponseEntity.ok(systemParameterService.add(body));
    }

    @PutMapping("/{paramId}")
    @SetupAuthorized
    @Operation(summary = "Update an existing system parameter", description = "Updates the system parameter with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "System parameter not found")
    })
    public ResponseEntity<BaseResponse<SystemParameter>> update(
            @Parameter(description = "ID of the system parameter to update", required = true)
            @PathVariable @Min(1) Integer paramId,
            @Valid @RequestBody SystemParameter body) {
        return ResponseEntity.ok(systemParameterService.update(paramId, body));
    }

    @GetMapping("/{paramId}")
    @SetupAuthorized
    @Operation(summary = "Get system parameter by ID", description = "Retrieves a system parameter by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "System parameter not found")
    })
    public ResponseEntity<BaseResponse<SystemParameter>> get(
            @Parameter(description = "ID of the system parameter to retrieve", required = true)
            @PathVariable @Min(1) Integer paramId) {
        return ResponseEntity.ok(systemParameterService.get(paramId));
    }

    @GetMapping
    @SetupAuthorized
    @Operation(summary = "List system parameters", description = "Retrieves a paginated list of system parameters with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameters retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<Page<SystemParameter>>> list(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Group filter for parameters")
            @RequestParam(required = false) String group,
            @Parameter(description = "Whether to return only active parameters")
            @RequestParam(required = false) Boolean onlyActive,
            @Parameter(description = "Whether to retrieve all parameters (ignores pagination)")
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged) {
        Pageable effectivePageable = unpaged ? Pageable.unpaged() : pageable;
        return ResponseEntity.ok(systemParameterService.list(effectivePageable, group, onlyActive));
    }

    @GetMapping("/by-key/{paramKey}")
    @SetupAuthorized
    @Operation(summary = "Get system parameter by key", description = "Retrieves a system parameter by its key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameter found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "System parameter not found")
    })
    public ResponseEntity<BaseResponse<SystemParameter>> getByKey(
            @Parameter(description = "Key of the parameter to retrieve", required = true)
            @PathVariable @NotBlank String paramKey) {
        return ResponseEntity.ok(systemParameterService.getByKey(paramKey));
    }

    @PostMapping("/by-keys")
    @SetupAuthorized
    @Operation(summary = "Get system parameters by keys", description = "Retrieves multiple system parameters by their keys")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "System parameters retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<SystemParameter>>> getByKeys(
            @Parameter(description = "List of parameter keys to retrieve", required = true)
            @RequestBody List<String> keys) {
        return ResponseEntity.ok(systemParameterService.getByKeys(keys));
    }
}
