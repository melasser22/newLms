package com.ejada.catalog.controller;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.service.TierFeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/tiers/{tierId}/features")
@RequiredArgsConstructor
@Validated
public class TierFeatureController {

    private final TierFeatureService service;

    @PostMapping
    public ResponseEntity<TierFeatureRes> attach(@PathVariable Integer tierId,
                                                 @Valid @RequestBody TierFeatureCreateReq req) {
        // ensure path id wins if body is absent/mismatched
        TierFeatureCreateReq normalized = new TierFeatureCreateReq(
            tierId,
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
        TierFeatureRes res = service.attach(normalized);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public TierFeatureRes update(@PathVariable Integer tierId,
                                 @PathVariable Integer id,
                                 @Valid @RequestBody TierFeatureUpdateReq req) {
        // tierId is only for routing; service validates existence internally
        return service.update(id, req);
    }

    @GetMapping
    public Page<TierFeatureRes> listByTier(@PathVariable Integer tierId,
                                           @ParameterObject Pageable pageable) {
        return service.listByTier(tierId, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> detach(@PathVariable Integer tierId, @PathVariable Integer id) {
        service.detach(id);
        return ResponseEntity.noContent().build();
    }
}
