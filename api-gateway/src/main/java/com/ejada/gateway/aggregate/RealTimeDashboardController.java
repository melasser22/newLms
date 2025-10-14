package com.ejada.gateway.aggregate;

import com.ejada.gateway.aggregate.RealTimeDashboardService.RealTimeSnapshot;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Exposes an SSE endpoint that streams aggregated dashboard metrics every five seconds. Consumers
 * receive the last successful aggregation even when partial downstream failures occur.
 */
@RestController
@RequestMapping(path = "/api/aggregate")
public class RealTimeDashboardController {

  private final RealTimeDashboardService realTimeDashboardService;

  public RealTimeDashboardController(RealTimeDashboardService realTimeDashboardService) {
    this.realTimeDashboardService = realTimeDashboardService;
  }

  @GetMapping(value = "/dashboard/{tenantId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<RealTimeSnapshot>> streamDashboard(@PathVariable Integer tenantId) {
    return realTimeDashboardService.stream(tenantId)
        .map(snapshot -> ServerSentEvent.<RealTimeSnapshot>builder(snapshot)
            .event(snapshot.healthy() ? "dashboard-metrics" : "dashboard-warning")
            .build());
  }
}
