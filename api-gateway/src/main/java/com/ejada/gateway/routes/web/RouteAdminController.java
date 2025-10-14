package com.ejada.gateway.routes.web;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteDefinitionRequest;
import com.ejada.gateway.routes.model.RouteManagementView;
import com.ejada.gateway.routes.model.RouteVariantMetricsView;
import com.ejada.gateway.routes.service.RouteDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/routes")
public class RouteAdminController {

  private final RouteDefinitionService routeService;

  public RouteAdminController(RouteDefinitionService routeService) {
    this.routeService = routeService;
  }

  @GetMapping
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<List<RouteDefinition>>> listRoutes() {
    return routeService.findAll()
        .collectList()
        .map(routes -> BaseResponse.success("Route definitions", routes));
  }

  @GetMapping("/ui")
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<List<RouteManagementView>>> uiSummary() {
    return routeService.fetchManagementView()
        .collectList()
        .map(views -> BaseResponse.success("Route overview", views));
  }

  @PostMapping("/validate")
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<RouteDefinition>> validateRoute(
      @Valid @RequestBody RouteDefinitionRequest request) {
    return routeService.validate(request)
        .map(route -> BaseResponse.success("Route is valid", route));
  }

  @PostMapping
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<RouteDefinition>> createRoute(@Valid @RequestBody RouteDefinitionRequest request,
      Authentication authentication) {
    return routeService.create(request, authentication)
        .map(route -> BaseResponse.success("Route created", route));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<RouteDefinition>> updateRoute(@PathVariable("id") UUID id,
      @Valid @RequestBody RouteDefinitionRequest request,
      Authentication authentication) {
    return routeService.update(id, request, authentication)
        .map(route -> BaseResponse.success("Route updated", route));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<Void>> disableRoute(@PathVariable("id") UUID id,
      Authentication authentication) {
    return routeService.delete(id, authentication)
        .thenReturn(BaseResponse.success("Route deleted", null));
  }

  @PostMapping("/{id}/bluegreen/{slot}")
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<RouteDefinition>> toggleBlueGreen(@PathVariable("id") UUID id,
      @PathVariable("slot") String slot,
      Authentication authentication) {
    String normalized = (slot == null) ? "" : slot.trim().toLowerCase();
    if (!"blue".equals(normalized) && !"green".equals(normalized)) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot must be 'blue' or 'green'"));
    }
    return routeService.toggleBlueGreen(id, normalized, authentication)
        .map(route -> BaseResponse.success("Blue/green slot toggled", route));
  }

  @GetMapping("/variants/metrics")
  @PreAuthorize("hasRole('EJADA_OFFICER')")
  public Mono<BaseResponse<List<RouteVariantMetricsView>>> variantMetrics() {
    return Mono.fromSupplier(routeService::variantMetrics)
        .map(metrics -> BaseResponse.success("Route variant metrics", metrics));
  }
}
