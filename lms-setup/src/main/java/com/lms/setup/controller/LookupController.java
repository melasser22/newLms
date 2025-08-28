package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Lookup;
import com.lms.setup.service.LookupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup/lookups")
@Validated
public class LookupController {

    private final LookupService lookupService;
    public LookupController(LookupService lookupService) { this.lookupService = lookupService; }

    // Kept name to align with previous code paths seen in your logs
    @GetMapping
    public ResponseEntity<BaseResponse<List<Lookup>>> getAllLookups() {
        return ResponseEntity.ok(lookupService.getAll());
    }

    @GetMapping("/group/{groupCode}")
    public ResponseEntity<BaseResponse<List<Lookup>>> getByGroup(@PathVariable String groupCode) {
        return ResponseEntity.ok(lookupService.getByGroup(groupCode));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Lookup>> add(@Valid @RequestBody Lookup body) {
        return ResponseEntity.ok(lookupService.add(body));
    }

    @PutMapping("/{lookupItemId}")
    public ResponseEntity<BaseResponse<Lookup>> update(@PathVariable Integer lookupItemId,
                                                       @Valid @RequestBody Lookup body) {
        return ResponseEntity.ok(lookupService.update(lookupItemId, body));
    }
}
