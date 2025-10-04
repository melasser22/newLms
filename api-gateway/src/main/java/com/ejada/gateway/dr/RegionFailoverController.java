package com.ejada.gateway.dr;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight control plane for operators to trigger failover or restoration.
 */
@RestController
@RequestMapping("/internal/dr")
public class RegionFailoverController {

  private final RegionFailoverService failoverService;

  public RegionFailoverController(RegionFailoverService failoverService) {
    this.failoverService = failoverService;
  }

  @PostMapping("/failover")
  public ResponseEntity<Map<String, String>> toggleFailover(@RequestBody Map<String, String> payload) {
    if (!failoverService.isEnabled()) {
      return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
          .body(Map.of("status", "disabled"));
    }
    String region = payload.getOrDefault("region", "");
    String reason = payload.getOrDefault("reason", "manual");
    String active;
    if (!StringUtils.hasText(region) || "primary".equalsIgnoreCase(region)) {
      active = failoverService.restorePrimary(reason);
    } else {
      active = failoverService.triggerFailover(reason + ":" + region);
    }
    return ResponseEntity.ok(Map.of("activeRegion", active));
  }
}
