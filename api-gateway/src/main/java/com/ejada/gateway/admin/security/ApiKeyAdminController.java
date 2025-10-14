package com.ejada.gateway.admin.security;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.admin.security.ApiKeyAdminService.ApiKeyCreatedResponse;
import com.ejada.gateway.admin.security.ApiKeyAdminService.ApiKeyRotationResponse;
import com.ejada.gateway.admin.security.ApiKeyAdminService.ApiKeyView;
import com.ejada.gateway.admin.security.ApiKeyAdminService.CreateApiKeyRequest;
import com.ejada.gateway.admin.security.ApiKeyAdminService.RotateApiKeyRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/admin/security/tenants/{tenantId}/api-keys")
public class ApiKeyAdminController {

  private final ApiKeyAdminService service;

  public ApiKeyAdminController(ApiKeyAdminService service) {
    this.service = service;
  }

  @GetMapping
  public Mono<BaseResponse<List<ApiKeyView>>> list(@PathVariable String tenantId) {
    return service.list(tenantId)
        .collectList()
        .map(list -> BaseResponse.success("API keys", list));
  }

  @PostMapping
  public Mono<BaseResponse<ApiKeyCreatedResponse>> create(@PathVariable String tenantId,
      @RequestBody CreateApiKeyRequest request) {
    return service.create(tenantId, request)
        .map(response -> BaseResponse.success("API key created", response));
  }

  @PostMapping("/{apiKey}/rotate")
  public Mono<BaseResponse<ApiKeyRotationResponse>> rotate(@PathVariable String tenantId,
      @PathVariable String apiKey,
      @RequestBody RotateApiKeyRequest request) {
    return service.rotate(tenantId, apiKey, request)
        .map(response -> BaseResponse.success("API key rotated", response));
  }

  @DeleteMapping("/{apiKey}")
  public Mono<BaseResponse<Void>> delete(@PathVariable String tenantId, @PathVariable String apiKey) {
    return service.delete(tenantId, apiKey)
        .thenReturn(BaseResponse.success("API key deleted", null));
  }
}
