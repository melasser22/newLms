package com.ejada.admin.controller;

import com.ejada.common.constants.ErrorCodes;
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
    return buildResponse(superadminService.createSuperadmin(request), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<BaseResponse<SuperadminDto>> updateSuperadmin(
      @PathVariable Long id,
      @Valid @RequestBody UpdateSuperadminRequest request) {
    return buildResponse(superadminService.updateSuperadmin(id, request), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> deleteSuperadmin(@PathVariable Long id) {
    return buildResponse(superadminService.deleteSuperadmin(id), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<SuperadminDto>> getSuperadmin(@PathVariable Long id) {
    return buildResponse(superadminService.getSuperadmin(id), HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<BaseResponse<Page<SuperadminDto>>> listSuperadmins(Pageable pageable) {
    return buildResponse(superadminService.listSuperadmins(pageable), HttpStatus.OK);
  }

  @PostMapping("/first-login")
  public ResponseEntity<BaseResponse<Void>> completeFirstLogin(
      @Valid @RequestBody FirstLoginRequest request) {
    return buildResponse(superadminService.completeFirstLogin(request), HttpStatus.OK);
  }

  @PostMapping("/change-password")
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
}
