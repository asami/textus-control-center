/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.time.Instant

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class LifecycleSupervisorProtocolSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Lifecycle supervisor protocol" should {
    "encode a stable request without a launch locator or credential" in {
      Given("a persisted Control Center lifecycle request")
      val request = LifecycleSupervisorRequest(
        "request-1",
        "idempotency-1",
        "textus-control-center",
        "start",
        "operator-1",
        Instant.parse("2026-07-22T00:00:05Z")
      )

      When("the post-commit continuation serializes it for the local supervisor")
      val body = LifecycleSupervisorProtocol.requestBody(request)

      Then("only the protocol identity and bounded request facts are emitted")
      body should include ("\"requestId\":\"request-1\"")
      body should include ("\"idempotencyKey\":\"idempotency-1\"")
      body should not include "directory"
      body should not include "token"
    }

    "resolve only the configured loopback supervisor request paths" in {
      Given("a loopback supervisor declaration")
      val configuration = LifecycleSupervisorConfiguration.fromProperties(Map(
        LifecycleSupervisorConfiguration.SUPERVISOR_ID -> "local-supervisor",
        LifecycleSupervisorConfiguration.ENDPOINT -> "http://127.0.0.1:18014",
        LifecycleSupervisorConfiguration.TOKEN_ENV -> "LIFECYCLE_TOKEN"
      )).toOption.flatten.getOrElse(fail("configuration is unavailable"))

      When("a continuation selects submit and reconciliation endpoints")
      val submit = LifecycleSupervisorProtocol.requestEndpoint(configuration)
      val lookup = LifecycleSupervisorProtocol.lookupEndpoint(configuration, "request-1")

      Then("both remain within the launcher-owned loopback protocol")
      submit.toString shouldBe "http://127.0.0.1:18014/v1/lifecycle-requests"
      lookup.toString shouldBe "http://127.0.0.1:18014/v1/lifecycle-requests/request-1"
    }

    "retain a missing credential as a safe local rejection" in {
      Given("a named but unavailable Control Center credential")
      val request = LifecycleSupervisorRequest("request-1", "key-1", "textus-control-center", "start", "operator-1", Instant.parse("2026-07-22T00:00:05Z"))

      When("the continuation cannot resolve the credential reference")
      val result = LifecycleSupervisorProtocol.unavailable(
        request,
        "local-supervisor",
        "supervisor-credential-unavailable",
        Instant.parse("2026-07-22T00:00:00Z")
      )

      Then("it creates no process authority and has a safe terminal diagnostic")
      result.state shouldBe "rejected"
      result.diagnosticCode shouldBe Some("supervisor-credential-unavailable")
      result.instanceId shouldBe empty
    }

    "reject a supervisor response for a different durable request" in {
      Given("a local supervisor response whose request identity differs from the persisted Control Center request")
      val body = """{"requestId":"other-request","state":"accepted","diagnosticCode":null,"diagnostic":null,"supervisorId":"local-supervisor","instanceId":"instance-1","acceptedAt":"2026-07-22T00:00:00Z","completedAt":null}"""

      When("the Control Center reconciles that response")
      val result = LifecycleSupervisorProtocol.response(body, "request-1")

      Then("it refuses to project another request's lifecycle result")
      result shouldBe Left("supervisor-response-request-mismatch")
    }
  }
}
