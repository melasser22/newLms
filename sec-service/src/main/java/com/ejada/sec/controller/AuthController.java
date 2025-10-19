package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseResponseController {

  private final AuthService authService;
  private final SuperadminService superadminService;

  private final PasswordResetService passwordResetService;

  @PostMapping("/register")
  public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
    return respond(() -> authService.register(req), HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest req) {
    return respond(() -> authService.login(req));
  }

  @PostMapping("/refresh")
  public ResponseEntity<BaseResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
    return respond(() -> authService.refresh(req));
  }

  @PostMapping("/logout")
  public ResponseEntity<BaseResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
    return respond(() -> authService.logout(request.getRefreshToken()));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<BaseResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
    return respond(() -> passwordResetService.createToken(req));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<BaseResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    return respond(() -> passwordResetService.reset(req));
  }

  @PostMapping("/admin/login")
  public ResponseEntity<BaseResponse<SuperadminAuthResponse>> Adminlogin(@Valid @RequestBody SuperadminLoginRequest request) {
    return respond(() -> superadminService.login(request));
  }
}
