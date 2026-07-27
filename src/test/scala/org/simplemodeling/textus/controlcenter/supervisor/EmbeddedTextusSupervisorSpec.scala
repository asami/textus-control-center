/*
 *  version Jul. 24, 2026
 * @version Jul. 27, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.time.Instant

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class EmbeddedTextusSupervisorSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Embedded Textus supervisor" should {
    "project a transition execution result as standalone supervisor authority" in {
      Given("a transition executor that accepted one lifecycle request")
      val request = _request("request-1")
      val supervisor = new EmbeddedTextusSupervisor(new TextusSupervisorTransitionExecutor {
        def submit(request: LifecycleSupervisorRequest): Either[String, LifecycleSupervisorResult] =
          Right(_accepted(request.requestId, "legacy-launcher-supervisor"))
        def lookup(requestId: String): Option[LifecycleSupervisorResult] =
          Some(_accepted(requestId, "legacy-launcher-supervisor"))
      })

      When("the embedded Control Center supervisor submits and looks up the request")
      val submitted = supervisor.submit(request, Instant.parse("2026-07-24T00:00:00Z"))
      val lookedup = supervisor.lookup(request.requestId)

      Then("the authoritative projection names the standalone Textus supervisor")
      submitted.supervisorId shouldBe EmbeddedTextusSupervisor.StandaloneSupervisorId
      lookedup.map(_.supervisorId) shouldBe Some(EmbeddedTextusSupervisor.StandaloneSupervisorId)
      submitted.instanceId shouldBe Some("instance-1")
    }

    "retain an unavailable transition execution as a safe Textus supervisor rejection" in {
      Given("a transition executor that cannot submit a lifecycle request")
      val request = _request("request-2")
      val supervisor = new EmbeddedTextusSupervisor(new TextusSupervisorTransitionExecutor {
        def submit(request: LifecycleSupervisorRequest): Either[String, LifecycleSupervisorResult] = Left("launcher-lifecycle-unavailable")
        def lookup(requestId: String): Option[LifecycleSupervisorResult] = None
      })

      When("the embedded supervisor receives the request")
      val result = supervisor.submit(request, Instant.parse("2026-07-24T00:00:00Z"))

      Then("it does not fabricate ownership and returns one safe rejection")
      result.state shouldBe "rejected"
      result.supervisorId shouldBe EmbeddedTextusSupervisor.StandaloneSupervisorId
      result.diagnosticCode shouldBe Some("launcher-lifecycle-unavailable")
      result.instanceId shouldBe empty
    }
  }

  private def _request(requestid: String): LifecycleSupervisorRequest =
    LifecycleSupervisorRequest(requestid, s"key-$requestid", "textus-control-center", "start", "operator-1", Instant.parse("2026-07-24T00:00:05Z"))

  private def _accepted(requestid: String, supervisorid: String): LifecycleSupervisorResult =
    LifecycleSupervisorResult(requestid, "accepted", None, None, supervisorid, Some("instance-1"), Some(Instant.parse("2026-07-24T00:00:00Z")), None)
}
