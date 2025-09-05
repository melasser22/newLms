package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.service.TierAddonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/tiers/{tierId}/addons")
@RequiredArgsConstructor
@Validated
public class TierAddonController {

    private final TierAddonService service;

    @PostMapping
    public ResponseEntity<TierAddonRes> allow(@PathVariable Integer tierId,
                                              @Valid @RequestBody TierAddonCreateReq req) {
        TierAddonCreateReq normalized = new TierAddonCreateReq(
            tierId,
            req.addonId(),
            req.included(),
            req.sortOrder(),
            req.basePrice(),
            req.currency()
        );
        TierAddonRes res = service.allow(normalized);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public TierAddonRes update(@PathVariable Integer tierId,
                               @PathVariable Integer id,
                               @Valid @RequestBody TierAddonUpdateReq req) {
        return service.update(id, req);
    }

    @GetMapping
    public Page<TierAddonRes> listByTier(@PathVariable Integer tierId,
                                         @ParameterObject Pageable pageable) {
        return service.listByTier(tierId, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Integer tierId, @PathVariable Integer id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }
}
