package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final PasswordResetService passwordResetService;

  @PostMapping("/register")
  public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
    return ResponseEntity.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest req) {
    return ResponseEntity.ok(authService.login(req));
  }

  @PostMapping("/refresh")
  public ResponseEntity<BaseResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
    return ResponseEntity.ok(authService.refresh(req));
  }

  @PostMapping("/logout")
  public ResponseEntity<BaseResponse<Void>> logout(@RequestParam("refreshToken") String refreshToken) {
    return ResponseEntity.ok(authService.logout(refreshToken));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<BaseResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
    // Return token for demo; in production, send via email/SMS and return 204
    return ResponseEntity.ok(passwordResetService.createToken(req));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<BaseResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    return ResponseEntity.ok(passwordResetService.reset(req));
  }
}
