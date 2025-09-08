package com.ejada.sec.controller;

import com.ejada.sec.dto.*;
import com.ejada.sec.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  @GetMapping
  public ResponseEntity<List<RoleDto>> list(@RequestParam("tenantId") UUID tenantId) {
    return ResponseEntity.ok(roleService.listByTenant(tenantId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<RoleDto> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(roleService.get(id));
  }

  @PostMapping
  public ResponseEntity<RoleDto> create(@Valid @RequestBody CreateRoleRequest req) {
    return ResponseEntity.ok(roleService.create(req));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<RoleDto> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateRoleRequest req) {
    return ResponseEntity.ok(roleService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    roleService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
