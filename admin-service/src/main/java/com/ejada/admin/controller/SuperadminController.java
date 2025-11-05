package com.ejada.admin.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.admin.dto.ChangePasswordRequest;
import com.ejada.admin.dto.CreateSuperadminRequest;
import com.ejada.admin.dto.FirstLoginRequest;
import com.ejada.admin.dto.SuperadminAuthResponse;
import com.ejada.admin.dto.SuperadminDto;
import com.ejada.admin.dto.SuperadminLoginRequest;
import com.ejada.admin.dto.UpdateSuperadminRequest;
import com.ejada.admin.security.SuperAdminAuthorized;
import com.ejada.admin.service.SuperadminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/superadmin")
@RequiredArgsConstructor
public class SuperadminController {

  private final SuperadminService superadminService;

  @PostMapping
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<SuperadminDto>> createSuperadmin(
      @Valid @RequestBody CreateSuperadminRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(superadminService.createSuperadmin(request));
  }

  @PutMapping("/{id}")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<SuperadminDto>> updateSuperadmin(
      @PathVariable Long id,
      @Valid @RequestBody UpdateSuperadminRequest request) {
    return ResponseEntity.ok(superadminService.updateSuperadmin(id, request));
  }

  @DeleteMapping("/{id}")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Void>> deleteSuperadmin(@PathVariable Long id) {
    return ResponseEntity.ok(superadminService.deleteSuperadmin(id));
  }

  @GetMapping("/{id}")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<SuperadminDto>> getSuperadmin(@PathVariable Long id) {
    return ResponseEntity.ok(superadminService.getSuperadmin(id));
  }

  @GetMapping
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Page<SuperadminDto>>> listSuperadmins(Pageable pageable) {
    return ResponseEntity.ok(superadminService.listSuperadmins(pageable));
  }

  @PostMapping("/complete-login")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Void>> completeFirstLogin(
      @Valid @RequestBody FirstLoginRequest request) {
    return ResponseEntity.ok(superadminService.completeFirstLogin(request));
  }

  @PostMapping("/change-password")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Void>> changeSuperadminPassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    return ResponseEntity.ok(superadminService.changePassword(request));
  }
  
  @PostMapping("/login")
  public ResponseEntity<BaseResponse<SuperadminAuthResponse>> Adminlogin(@Valid @RequestBody SuperadminLoginRequest request) {
    return ResponseEntity.ok( superadminService.login(request));
  }
}
