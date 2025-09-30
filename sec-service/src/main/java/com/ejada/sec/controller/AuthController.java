package com.ejada.sec.controller;

import static com.ejada.common.http.BaseResponseEntityFactory.build;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.dto.admin.SuperadminAuthResponse;
import com.ejada.sec.dto.admin.SuperadminLoginRequest;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.PasswordResetService;
import com.ejada.sec.service.SuperadminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    return build(response, HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest req) {
    BaseResponse<AuthResponse> response = authService.login(req);
    return build(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<BaseResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
    BaseResponse<AuthResponse> response = authService.refresh(req);
    return build(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<BaseResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
    BaseResponse<Void> response = authService.logout(request.getRefreshToken());
    return build(response);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<BaseResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
    BaseResponse<Void> response = passwordResetService.createToken(req);
    return build(response);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<BaseResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    BaseResponse<Void> response = passwordResetService.reset(req);
    return build(response);
  }
  
  @PostMapping("admin/login")
  public ResponseEntity<BaseResponse<SuperadminAuthResponse>> Adminlogin(@Valid @RequestBody SuperadminLoginRequest request) {
    BaseResponse<SuperadminAuthResponse> response = superadminService.login(request);
    return build(response);
  }

}
