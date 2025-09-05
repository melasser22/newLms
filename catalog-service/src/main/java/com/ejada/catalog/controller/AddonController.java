package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.service.AddonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/addons")
@RequiredArgsConstructor
@Validated
public class AddonController {

    private final AddonService service;

    @PostMapping
    public ResponseEntity<AddonRes> create(@Valid @RequestBody AddonCreateReq req) {
        AddonRes res = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public AddonRes update(@PathVariable Integer id, @Valid @RequestBody AddonUpdateReq req) {
        return service.update(id, req);
    }

    @GetMapping("/{id}")
    public AddonRes get(@PathVariable Integer id) {
        return service.get(id);
    }

    @GetMapping
    public Page<AddonRes> list(@RequestParam(required = false) String category,
                               @ParameterObject Pageable pageable) {
        return service.list(category, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
