package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.ResourceDto;
import com.ejada.setup.security.SetupAuthorized;
import com.ejada.setup.service.ResourceService;
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
import com.ejada.setup.constants.ValidationConstants;

import java.util.List;

@RestController
@RequestMapping("/setup/resources")
@Validated
@Tag(name = "Resource Management", description = "APIs for managing resources")
public final class ResourceController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed by Spring")
    private final ResourceService resourceService;

    public ResourceController(final ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @SetupAuthorized
    @Operation(summary = "Create a new resource", description = "Creates a new resource with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Resource already exists")
    })
    public ResponseEntity<BaseResponse<ResourceDto>> add(final @Valid @RequestBody ResourceDto body) {
        return ResponseEntity.ok(resourceService.add(body));
    }

    @PutMapping("/{resourceId}")
    @SetupAuthorized
    @Operation(summary = "Update an existing resource", description = "Updates the resource with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<BaseResponse<ResourceDto>> update(
            @Parameter(description = "ID of the resource to update", required = true)
            @PathVariable @Min(1) final Integer resourceId,
            final @Valid @RequestBody ResourceDto body) {
        return ResponseEntity.ok(resourceService.update(resourceId, body));
    }

    @GetMapping("/{resourceId}")
    @SetupAuthorized
    @Operation(summary = "Get resource by ID", description = "Retrieves a resource by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<BaseResponse<ResourceDto>> get(
            @Parameter(description = "ID of the resource to retrieve", required = true)
            @PathVariable @Min(1) final Integer resourceId) {
        return ResponseEntity.ok(resourceService.get(resourceId));
    }

    @GetMapping
    @SetupAuthorized
    @Operation(summary = "List resources", description = "Retrieves a paginated list of resources with optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resources retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<Page<ResourceDto>>> list(
            @PageableDefault(size = ValidationConstants.PAGE_SIZE_DEFAULT) final Pageable pageable,
            @Parameter(description = "Search query for resource names")
            final @RequestParam(required = false) String q,
            @Parameter(description = "Whether to retrieve all resources (ignores pagination)")
            @RequestParam(name = "unpaged", defaultValue = "false") final boolean unpaged) {
        final Pageable effectivePageable = unpaged ? Pageable.unpaged() : pageable;
        return ResponseEntity.ok(resourceService.list(effectivePageable, q, unpaged));
    }

    @GetMapping("/active")
    @SetupAuthorized
    @Operation(summary = "List active resources", description = "Retrieves all active resources")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active resources retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BaseResponse<List<ResourceDto>>> listActive() {
        return ResponseEntity.ok(resourceService.listActive());
    }
}
