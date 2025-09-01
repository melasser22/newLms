package com.lms.tenant.web;

import com.lms.tenant.core.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @PutMapping("/{id}/overage-policy/enabled/{flag}")
    public ResponseEntity<Void> overage(@PathVariable UUID id, @PathVariable boolean flag) {
        service.toggleOverage(id, flag);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/settings")
    public ResponseEntity<Boolean> settings(@PathVariable UUID id) {
        return ResponseEntity.ok(service.isOverageEnabled(id));
    }
}
