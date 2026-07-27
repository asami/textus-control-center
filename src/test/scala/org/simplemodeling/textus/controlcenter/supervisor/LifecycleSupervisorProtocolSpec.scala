/*
 *  version Jul. 24, 2026
 * @version Jul. 27, 2026
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

    "retain a transition executor failure as a safe Textus supervisor rejection" in {
      Given("a lifecycle request whose transition executor cannot be reached")
      val request = LifecycleSupervisorRequest("request-1", "key-1", "textus-control-center", "start", "operator-1", Instant.parse("2026-07-22T00:00:05Z"))

      When("the embedded Textus supervisor receives an authority failure")
      val result = LifecycleSupervisorProtocol.unavailable(
        request,
        "",
        "supervisor-authority-unavailable",
        Instant.parse("2026-07-22T00:00:00Z")
      )

      Then("it carries no process locator and has a safe terminal diagnostic")
      result.state shouldBe "rejected"
      result.diagnosticCode shouldBe Some("supervisor-authority-unavailable")
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

    "project a matching accepted supervisor result without private request facts" in {
      Given("a matching accepted response from the configured local supervisor")
      val body = """{"requestId":"request-1","state":"accepted","diagnosticCode":null,"diagnostic":null,"supervisorId":"local-supervisor","instanceId":"instance-1","acceptedAt":"2026-07-22T00:00:00Z","completedAt":null}"""

      When("the Control Center reconciles its persisted request")
      val result = LifecycleSupervisorProtocol.response(body, "request-1")

      Then("it retains only the safe receiver, instance, state, and timing facts")
      result.map(_.supervisorId) shouldBe Right("local-supervisor")
      result.map(_.instanceId) shouldBe Right(Some("instance-1"))
      result.map(_.state) shouldBe Right("accepted")
    }
  }
}
