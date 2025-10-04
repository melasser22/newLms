package com.ejada.gateway.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Gatling simulation capturing throughput, latency and error rates under
 * multiple load profiles. The scenarios can be triggered via the
 * gatling-maven-plugin during CI pipelines to guard against performance
 * regressions introduced by routing or filter changes.
 */
class GatewayLoadSimulation extends Simulation {

  private val baseUrl = System.getProperty("gateway.baseUrl", "http://localhost:8080")
  private val tenantCount = Integer.getInteger("gateway.tenants", 5)

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .header("Authorization", "Bearer performance-token")

  private val tenantFeeder = Iterator.continually {
    val tenantId = (1 + util.Random.nextInt(tenantCount)).toString
    Map("tenantId" -> tenantId)
  }

  private val steadyStateScenario = scenario("steady-state-load")
    .feed(tenantFeeder)
    .exec(
      http("dashboard-request")
        .get("/api/bff/tenants/${tenantId}/dashboard")
        .header("X-Tenant-Id", session => session("tenantId").as[String])
        .check(status.in(200, 304, 429))
    )

  private val burstScenario = scenario("burst-load")
    .feed(tenantFeeder)
    .during(20.seconds) {
      exec(
        http("burst-request")
          .get("/api/bff/tenants/${tenantId}/dashboard")
          .header("X-Tenant-Id", session => session("tenantId").as[String])
          .check(status.in(200, 304, 429))
      )
    }

  setUp(
    steadyStateScenario.inject(rampUsersPerSec(1).to(20).during(2.minutes)),
    burstScenario.inject(atOnceUsers(100))
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile4.lt(2000),
      global.successfulRequests.percent.gt(95)
    )
}
