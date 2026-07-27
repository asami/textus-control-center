/*
 * @version Jul. 28, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.time.Instant

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.supervisor.{Supervisor, SupervisorRequest, SupervisorResult, SupervisorState}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class EmbeddedTextusSupervisorSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Embedded Textus supervisor" should {
    "project the CNCF Supervisor SPI result as standalone supervisor authority" in {
      Given("an embedded CNCF Supervisor provider that accepted one lifecycle request")
      val request = _request("request-1")
      val provider = new Supervisor {
        def submit(request: SupervisorRequest)(using ExecutionContext): Consequence[SupervisorResult] =
          Consequence.success(_accepted(request.requestId))
        def lookup(requestId: String)(using ExecutionContext): Consequence[Option[SupervisorResult]] =
          Consequence.success(Some(_accepted(requestId)))
      }
      val supervisor = new EmbeddedTextusSupervisor(provider, ExecutionContext.create())

      When("the embedded Control Center supervisor submits and looks up the request")
      val submitted = supervisor.submit(request, Instant.parse("2026-07-24T00:00:00Z"))
      val lookedup = supervisor.lookup(request.requestId)

      Then("the authoritative projection preserves the selected provider identity")
      submitted.supervisorId shouldBe "provider-supervisor"
      lookedup.map(_.supervisorId) shouldBe Some("provider-supervisor")
      submitted.instanceId shouldBe Some("instance-1")
    }

    "retain an unavailable SPI provider result as a safe Textus supervisor rejection" in {
      Given("an embedded CNCF Supervisor provider that cannot submit a lifecycle request")
      val request = _request("request-2")
      val provider = new Supervisor {
        def submit(request: SupervisorRequest)(using ExecutionContext): Consequence[SupervisorResult] =
          Consequence.serviceUnavailable("provider unavailable")
        def lookup(requestId: String)(using ExecutionContext): Consequence[Option[SupervisorResult]] =
          Consequence.success(None)
      }
      val supervisor = new EmbeddedTextusSupervisor(provider, ExecutionContext.create())

      When("the embedded supervisor receives the request")
      val result = supervisor.submit(request, Instant.parse("2026-07-24T00:00:00Z"))

      Then("it does not fabricate ownership and returns one safe rejection")
      result.state shouldBe "rejected"
      result.supervisorId shouldBe EmbeddedTextusSupervisor.StandaloneSupervisorId
      result.diagnosticCode shouldBe Some("textus-supervisor-unavailable")
      result.instanceId shouldBe empty
    }
  }

  private def _request(requestid: String): LifecycleSupervisorRequest =
    LifecycleSupervisorRequest(requestid, s"key-$requestid", "textus-control-center", "start", "operator-1", Instant.parse("2026-07-24T00:00:05Z"))

  private def _accepted(requestid: String): SupervisorResult =
    SupervisorResult(
      requestid,
      SupervisorState.Accepted,
      None,
      None,
      "provider-supervisor",
      Some("instance-1"),
      Some(Instant.parse("2026-07-24T00:00:00Z")),
      None
    )
}
