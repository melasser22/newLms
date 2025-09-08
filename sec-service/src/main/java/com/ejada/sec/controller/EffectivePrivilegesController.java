package com.ejada.sec.controller;

import com.ejada.sec.domain.EffectivePrivilegeProjection;
import com.ejada.sec.repository.EffectivePrivilegeViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/effective-privileges")
@RequiredArgsConstructor
public class EffectivePrivilegesController {

  private final EffectivePrivilegeViewRepository viewRepo;

  @GetMapping("/{userId}")
  public ResponseEntity<List<EffectivePrivilegeProjection>> list(@PathVariable Long userId,
                                                                 @RequestParam("tenantId") UUID tenantId) {
    return ResponseEntity.ok(viewRepo.findEffectiveByUserAndTenant(userId, tenantId));
  }
}
