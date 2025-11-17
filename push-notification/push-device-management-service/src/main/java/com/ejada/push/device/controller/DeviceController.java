package com.ejada.push.device.controller;

import com.ejada.push.device.model.DeviceRegistration;
import com.ejada.push.device.service.DeviceRegistrationRequest;
import com.ejada.push.device.service.DeviceRegistrationResponse;
import com.ejada.push.device.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/devices")
public class DeviceController {

  private final DeviceService deviceService;

  public DeviceController(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DeviceRegistrationResponse register(
      @PathVariable String tenantId, @Valid @RequestBody DeviceRegistrationRequest request) {
    return deviceService.register(tenantId, request);
  }

  @PutMapping("/{token}/refresh")
  public DeviceRegistrationResponse refresh(@PathVariable String tenantId, @PathVariable String token) {
    return deviceService.refresh(tenantId, token);
  }

  @DeleteMapping("/{token}")
  public DeviceRegistrationResponse revoke(@PathVariable String tenantId, @PathVariable String token) {
    return deviceService.revoke(tenantId, token);
  }

  @GetMapping
  public List<DeviceRegistration> listActive(@PathVariable String tenantId) {
    return deviceService.listActive(tenantId);
  }
}
