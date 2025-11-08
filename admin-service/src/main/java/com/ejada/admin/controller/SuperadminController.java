package com.ejada.admin.controller;

import com.ejada.common.constants.ErrorCodes;
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
    return buildResponse(superadminService.createSuperadmin(request), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<SuperadminDto>> updateSuperadmin(
      @PathVariable Long id,
      @Valid @RequestBody UpdateSuperadminRequest request) {
    return buildResponse(superadminService.updateSuperadmin(id, request), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Void>> deleteSuperadmin(@PathVariable Long id) {
    return buildResponse(superadminService.deleteSuperadmin(id), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<SuperadminDto>> getSuperadmin(@PathVariable Long id) {
    return buildResponse(superadminService.getSuperadmin(id), HttpStatus.OK);
  }

  @GetMapping
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Page<SuperadminDto>>> listSuperadmins(Pageable pageable) {
    return buildResponse(superadminService.listSuperadmins(pageable), HttpStatus.OK);
  }

  @PostMapping("/complete-login")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Void>> completeFirstLogin(
      @Valid @RequestBody FirstLoginRequest request) {
    return buildResponse(superadminService.completeFirstLogin(request), HttpStatus.OK);
  }

  @PostMapping("/change-password")
  @SuperAdminAuthorized
  public ResponseEntity<BaseResponse<Void>> changeSuperadminPassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    return buildResponse(superadminService.changePassword(request), HttpStatus.OK);
  }

  private <T> ResponseEntity<BaseResponse<T>> buildResponse(
      BaseResponse<T> response,
      HttpStatus successStatus) {
    if (response == null || response.isSuccess()) {
      return ResponseEntity.status(successStatus).body(response);
    }

    HttpStatus errorStatus = ErrorCodes.NOT_FOUND.equals(response.getCode())
        ? HttpStatus.NOT_FOUND
        : HttpStatus.BAD_REQUEST;

    return ResponseEntity.status(errorStatus).body(response);
  }
  
  @PostMapping("/login")
  public ResponseEntity<BaseResponse<SuperadminAuthResponse>> Adminlogin(@Valid @RequestBody SuperadminLoginRequest request) {
    return ResponseEntity.ok( superadminService.login(request));
  }
}
