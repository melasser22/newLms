package com.ejada.sec.controller;

import com.ejada.sec.dto.*;
import com.ejada.sec.service.PrivilegeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/privileges")
@RequiredArgsConstructor
public class PrivilegeController {

  private final PrivilegeService privilegeService;

  @GetMapping
  public ResponseEntity<List<PrivilegeDto>> list(@RequestParam("tenantId") UUID tenantId) {
    return ResponseEntity.ok(privilegeService.listByTenant(tenantId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PrivilegeDto> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(privilegeService.get(id));
  }

  @PostMapping
  public ResponseEntity<PrivilegeDto> create(@Valid @RequestBody CreatePrivilegeRequest req) {
    return ResponseEntity.ok(privilegeService.create(req));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<PrivilegeDto> update(@PathVariable("id") Long id,
                                             @Valid @RequestBody UpdatePrivilegeRequest req) {
    return ResponseEntity.ok(privilegeService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    privilegeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
