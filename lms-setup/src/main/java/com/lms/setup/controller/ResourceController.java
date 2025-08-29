package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Resource;
import com.lms.setup.service.ResourceService;
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
@RequestMapping("/setup/resources")
@Validated
@Tag(name = "Resource Management", description = "APIs for managing resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new resource", description = "Creates a new resource with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource created successfully",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Resource already exists")
    })
    public ResponseEntity<BaseResponse<Resource>> add(@Valid @RequestBody Resource body) {
        return ResponseEntity.ok(resourceService.add(body));
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing resource", description = "Updates the resource with the specified ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<BaseResponse<Resource>> update(
            @Parameter(description = "ID of the resource to update", required = true)
            @PathVariable @Min(1) Integer resourceId,
            @Valid @RequestBody Resource body) {
        return ResponseEntity.ok(resourceService.update(resourceId, body));
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get resource by ID", description = "Retrieves a resource by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource found successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<BaseResponse<Resource>> get(
            @Parameter(description = "ID of the resource to retrieve", required = true)
            @PathVariable @Min(1) Integer resourceId) {
        return ResponseEntity.ok(resourceService.get(resourceId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "List resources", description = "Retrieves a paginated list of resources with optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resources retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<?> list(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Search query for resource names")
            @RequestParam(required = false) String q,
            @Parameter(description = "Whether to return all resources (ignores pagination)")
            @RequestParam(required = false) boolean all) {
        return ResponseEntity.ok(resourceService.list(pageable, q, all));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "List active resources", description = "Retrieves all active resources")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active resources retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<?> listActive() {
        return ResponseEntity.ok(resourceService.listActive());
    }
}
