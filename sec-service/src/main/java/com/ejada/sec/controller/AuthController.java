package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.ApiStatusMapper;
import com.ejada.sec.dto.*;
import com.ejada.sec.dto.admin.SuperadminAuthResponse;
import com.ejada.sec.dto.admin.SuperadminLoginRequest;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.PasswordResetService;
import com.ejada.sec.service.SuperadminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final SuperadminService superadminService;

  private final PasswordResetService passwordResetService;

  @PostMapping("/register")
  public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
    BaseResponse<AuthResponse> response = authService.register(req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest req) {
    BaseResponse<AuthResponse> response = authService.login(req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<BaseResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
    BaseResponse<AuthResponse> response = authService.refresh(req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<BaseResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
    BaseResponse<Void> response = authService.logout(request.getRefreshToken());
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<BaseResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
    BaseResponse<Void> response = passwordResetService.createToken(req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<BaseResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    BaseResponse<Void> response = passwordResetService.reset(req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }
  
  @PostMapping("admin/login")
  public ResponseEntity<BaseResponse<SuperadminAuthResponse>> Adminlogin(@Valid @RequestBody SuperadminLoginRequest request) {
    BaseResponse<SuperadminAuthResponse> response = superadminService.login(request);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

}
