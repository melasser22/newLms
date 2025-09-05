package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.service.AddonFeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/addons/{addonId}/features")
@RequiredArgsConstructor
@Validated
public class AddonFeatureController {

    private final AddonFeatureService service;

    @PostMapping
    public ResponseEntity<AddonFeatureRes> attach(@PathVariable Integer addonId,
                                                  @Valid @RequestBody AddonFeatureCreateReq req) {
        AddonFeatureCreateReq normalized = new AddonFeatureCreateReq(
            addonId,
            req.featureId(),
            req.enabled(),
            req.enforcement(),
            req.softLimit(),
            req.hardLimit(),
            req.limitWindow(),
            req.measureUnit(),
            req.resetCron(),
            req.overageEnabled(),
            req.overageUnitPrice(),
            req.overageCurrency(),
            req.meta()
        );
        AddonFeatureRes res = service.attach(normalized);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public AddonFeatureRes update(@PathVariable Integer addonId,
                                  @PathVariable Integer id,
                                  @Valid @RequestBody AddonFeatureUpdateReq req) {
        return service.update(id, req);
    }

    @GetMapping
    public Page<AddonFeatureRes> listByAddon(@PathVariable Integer addonId,
                                             @ParameterObject Pageable pageable) {
        return service.listByAddon(addonId, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> detach(@PathVariable Integer addonId, @PathVariable Integer id) {
        service.detach(id);
        return ResponseEntity.noContent().build();
    }
}
