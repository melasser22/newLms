package com.ejada.push.device.service;

import com.ejada.push.device.model.DeviceRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

  private final Map<String, Map<String, DeviceRegistration>> devicesByTenant = new ConcurrentHashMap<>();

  public DeviceRegistrationResponse register(String tenantId, DeviceRegistrationRequest request) {
    devicesByTenant.putIfAbsent(tenantId, new ConcurrentHashMap<>());
    Map<String, DeviceRegistration> tenantDevices = devicesByTenant.get(tenantId);
    DeviceRegistration registration =
        tenantDevices.compute(
            request.deviceToken(),
            (token, existing) -> {
              if (existing == null) {
                return new DeviceRegistration(
                    request.userId(), request.deviceToken(), request.platform(), request.appId());
              }
              existing.refresh();
              return existing;
            });
    return new DeviceRegistrationResponse(registration.getDeviceToken(), registration.isActive() ? "ACTIVE" : "INACTIVE");
  }

  public DeviceRegistrationResponse refresh(String tenantId, String deviceToken) {
    Map<String, DeviceRegistration> tenantDevices = devicesByTenant.get(tenantId);
    if (tenantDevices == null || !tenantDevices.containsKey(deviceToken)) {
      return new DeviceRegistrationResponse(deviceToken, "UNKNOWN");
    }
    tenantDevices.get(deviceToken).refresh();
    return new DeviceRegistrationResponse(deviceToken, "ACTIVE");
  }

  public DeviceRegistrationResponse revoke(String tenantId, String deviceToken) {
    Map<String, DeviceRegistration> tenantDevices = devicesByTenant.get(tenantId);
    if (tenantDevices == null || !tenantDevices.containsKey(deviceToken)) {
      return new DeviceRegistrationResponse(deviceToken, "UNKNOWN");
    }
    tenantDevices.get(deviceToken).deactivate();
    return new DeviceRegistrationResponse(deviceToken, "INACTIVE");
  }

  public List<DeviceRegistration> listActive(String tenantId) {
    Map<String, DeviceRegistration> tenantDevices = devicesByTenant.get(tenantId);
    if (tenantDevices == null) {
      return List.of();
    }
    List<DeviceRegistration> result = new ArrayList<>();
    for (DeviceRegistration registration : tenantDevices.values()) {
      if (registration.isActive()) {
        result.add(registration);
      }
    }
    return result;
  }
}
