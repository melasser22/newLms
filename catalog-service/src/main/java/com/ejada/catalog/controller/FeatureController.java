package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.service.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/features")
@RequiredArgsConstructor
@Validated
public class FeatureController {

    private final FeatureService service;

    @PostMapping
    public ResponseEntity<FeatureRes> create(@Valid @RequestBody FeatureCreateReq req) {
        FeatureRes res = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public FeatureRes update(@PathVariable Integer id, @Valid @RequestBody FeatureUpdateReq req) {
        return service.update(id, req);
    }

    @GetMapping("/{id}")
    public FeatureRes get(@PathVariable Integer id) {
        return service.get(id);
    }

    @GetMapping
    public Page<FeatureRes> list(@RequestParam(required = false) String category,
                                 @ParameterObject Pageable pageable) {
        return service.list(category, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
