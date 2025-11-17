package com.ejada.push.management.service;

import com.ejada.push.management.config.ChildServiceProperties;
import com.ejada.push.management.dto.DeviceRegistrationRequest;
import com.ejada.push.management.dto.DeviceRegistrationResponse;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class DeviceGatewayService {

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public DeviceGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = properties;
    this.invoker = invoker;
  }

  public DeviceRegistrationResponse register(String tenantId, DeviceRegistrationRequest request) {
    URI baseUrl = requireEndpoint(properties.getDevice().getBaseUrl(), "device");
    return invoker.post(
        baseUrl,
        "/api/v1/tenants/" + tenantId + "/devices",
        request,
        DeviceRegistrationResponse.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
