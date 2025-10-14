package com.ejada.gateway.admin.security;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.security.TenantIpWhitelistService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/admin/security/tenants/{tenantId}/ip-whitelist")
public class TenantIpWhitelistController {

  private final TenantIpWhitelistService service;

  public TenantIpWhitelistController(TenantIpWhitelistService service) {
    this.service = service;
  }

  @GetMapping
  public Mono<BaseResponse<List<String>>> list(@PathVariable String tenantId) {
    return service.list(tenantId)
        .collectList()
        .map(list -> BaseResponse.success("Tenant IP whitelist", list));
  }

  @PutMapping
  public Mono<BaseResponse<Void>> replace(@PathVariable String tenantId,
      @RequestBody WhitelistUpdateRequest request) {
    List<String> entries = request != null ? request.entries() : List.of();
    return service.replace(tenantId, entries)
        .thenReturn(BaseResponse.success("Whitelist updated", null));
  }

  @PostMapping
  public Mono<BaseResponse<Void>> add(@PathVariable String tenantId, @RequestBody Map<String, String> payload) {
    String entry = payload != null ? payload.getOrDefault("entry", null) : null;
    return service.add(tenantId, entry)
        .thenReturn(BaseResponse.success("Whitelist entry added", null));
  }

  @DeleteMapping
  public Mono<BaseResponse<Void>> remove(@PathVariable String tenantId, @RequestBody Map<String, String> payload) {
    String entry = payload != null ? payload.getOrDefault("entry", null) : null;
    return service.remove(tenantId, entry)
        .thenReturn(BaseResponse.success("Whitelist entry removed", null));
  }

  public record WhitelistUpdateRequest(List<String> entries) { }
}
