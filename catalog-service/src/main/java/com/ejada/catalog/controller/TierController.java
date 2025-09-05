package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.service.TierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/tiers")
@RequiredArgsConstructor
@Validated
public class TierController {

    private final TierService service;

    @PostMapping
    public ResponseEntity<TierRes> create(@Valid @RequestBody TierCreateReq req) {
        TierRes res = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public TierRes update(@PathVariable Integer id, @Valid @RequestBody TierUpdateReq req) {
        return service.update(id, req);
    }

    @GetMapping("/{id}")
    public TierRes get(@PathVariable Integer id) {
        return service.get(id);
    }

    @GetMapping
    public Page<TierRes> list(@RequestParam(required = false) Boolean active,
                              @ParameterObject Pageable pageable) {
        return service.list(active, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
