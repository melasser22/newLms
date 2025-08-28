
package com.shared.actuator.starter.endpoints;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Endpoint(id = "whoami")
public class WhoAmIEndpoint {

  @ReadOperation
  public Map<String, Object> whoami() {
    Map<String, Object> m = new LinkedHashMap<>();
    try {
      var runtime = ManagementFactory.getRuntimeMXBean();
      m.put("pid", runtime.getPid());
      m.put("uptime", Duration.ofMillis(runtime.getUptime()).toString());
      var host = InetAddress.getLocalHost();
      m.put("hostname", host.getHostName());
      m.put("hostaddr", host.getHostAddress());
      m.put("application", System.getProperty("spring.application.name", "app"));
      m.put("region", System.getenv().getOrDefault("REGION", "unknown"));
      m.put("zone", System.getenv().getOrDefault("ZONE", "unknown"));
      m.put("pod", System.getenv().getOrDefault("POD_NAME", "unknown"));
      m.put("node", System.getenv().getOrDefault("NODE_NAME", "unknown"));
    } catch (Exception e) {
      m.put("error", e.getMessage());
    }
    return m;
  }
}
