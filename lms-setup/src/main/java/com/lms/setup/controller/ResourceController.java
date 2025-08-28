package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Resource;
import com.lms.setup.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setup/resources")
@Validated
public class ResourceController {

    private final ResourceService resourceService;
    public ResourceController(ResourceService resourceService) { this.resourceService = resourceService; }

    @PostMapping
    public ResponseEntity<BaseResponse<Resource>> add(@Valid @RequestBody Resource body) {
        return ResponseEntity.ok(resourceService.add(body));
    }

    @PutMapping("/{resourceId}")
    public ResponseEntity<BaseResponse<Resource>> update(@PathVariable Integer resourceId,
                                                         @Valid @RequestBody Resource body) {
        return ResponseEntity.ok(resourceService.update(resourceId, body));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<BaseResponse<Resource>> get(@PathVariable Integer resourceId) {
        return ResponseEntity.ok(resourceService.get(resourceId));
    }

    @GetMapping
    public ResponseEntity<?> list(@PageableDefault(size = 20) Pageable pageable,
                                  @RequestParam(required = false) String q) {
        return ResponseEntity.ok(resourceService.list(pageable, q));
    }

    // Children of a resource (cached)
    @GetMapping("/{parentResourceId}/children")
    public ResponseEntity<?> children(@PathVariable Integer parentResourceId) {
        return ResponseEntity.ok(resourceService.childrenOf(parentResourceId));
    }
}
