package com.ejada.admin.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.admin.dto.ChangePasswordRequest;
import com.ejada.admin.dto.CreateSuperadminRequest;
import com.ejada.admin.dto.FirstLoginRequest;
import com.ejada.admin.dto.SuperadminDto;
import com.ejada.admin.dto.UpdateSuperadminRequest;
import com.ejada.admin.service.SuperadminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admins")
@RequiredArgsConstructor
@PreAuthorize("@authorizationExpressions.isEjadaOfficer(authentication)")
public class SuperadminController {

  private final SuperadminService superadminService;

  @PostMapping
  public ResponseEntity<BaseResponse<SuperadminDto>> createSuperadmin(
      @Valid @RequestBody CreateSuperadminRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(superadminService.createSuperadmin(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<BaseResponse<SuperadminDto>> updateSuperadmin(
      @PathVariable Long id,
      @Valid @RequestBody UpdateSuperadminRequest request) {
    return ResponseEntity.ok(superadminService.updateSuperadmin(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> deleteSuperadmin(@PathVariable Long id) {
    return ResponseEntity.ok(superadminService.deleteSuperadmin(id));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<SuperadminDto>> getSuperadmin(@PathVariable Long id) {
    return ResponseEntity.ok(superadminService.getSuperadmin(id));
  }

  @GetMapping
  public ResponseEntity<BaseResponse<Page<SuperadminDto>>> listSuperadmins(Pageable pageable) {
    return ResponseEntity.ok(superadminService.listSuperadmins(pageable));
  }

  @PostMapping("/first-login")
  public ResponseEntity<BaseResponse<Void>> completeFirstLogin(
      @Valid @RequestBody FirstLoginRequest request) {
    return ResponseEntity.ok(superadminService.completeFirstLogin(request));
  }

  @PostMapping("/change-password")
  public ResponseEntity<BaseResponse<Void>> changeSuperadminPassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    return ResponseEntity.ok(superadminService.changePassword(request));
  }
}
