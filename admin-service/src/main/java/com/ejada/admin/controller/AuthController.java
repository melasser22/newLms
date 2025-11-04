package com.ejada.admin.controller;

import com.ejada.admin.dto.SuperadminAuthResponse;
import com.ejada.admin.service.SuperadminService;
import com.ejada.common.dto.BaseResponse;

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
public class AuthController  {

  private final SuperadminService superadminService;

  
  @PostMapping("/admin/login")
  public ResponseEntity<BaseResponse<SuperadminAuthResponse>> Adminlogin(@Valid @RequestBody SuperadminLoginRequest request) {
    return ResponseEntity.ok( superadminService.login(request));
  }
}
