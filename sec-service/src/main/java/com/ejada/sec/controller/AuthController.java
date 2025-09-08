package com.ejada.sec.controller;

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
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
    return ResponseEntity.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
    return ResponseEntity.ok(authService.login(req));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
    return ResponseEntity.ok(authService.refresh(req));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestParam("refreshToken") String refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
    // Return token for demo; in production, send via email/SMS and return 204
    String token = passwordResetService.createToken(req);
    return ResponseEntity.ok(token);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    passwordResetService.reset(req);
    return ResponseEntity.noContent().build();
  }
}
