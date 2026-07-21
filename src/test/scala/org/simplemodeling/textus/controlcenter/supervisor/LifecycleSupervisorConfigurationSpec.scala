/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.time.Duration

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LifecycleSupervisorConfigurationSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Lifecycle supervisor configuration" should {
    "leave dispatch unavailable when no supervisor declaration exists" in {
      Given("an ordinary standalone Control Center configuration")

      When("no lifecycle-supervisor keys are present")
      val result = LifecycleSupervisorConfiguration.fromProperties(Map.empty)

      Then("the runtime can persist a safe not-configured result")
      result shouldBe Right(None)
    }

    "accept a bounded loopback supervisor with a credential reference" in {
      Given("a local supervisor declaration without a credential value")
      val values = Map(
        LifecycleSupervisorConfiguration.SUPERVISOR_ID -> "local-cncf-launcher",
        LifecycleSupervisorConfiguration.ENDPOINT -> "http://127.0.0.1:19400",
        LifecycleSupervisorConfiguration.TIMEOUT -> "2000ms",
        LifecycleSupervisorConfiguration.TOKEN_ENV -> "TEXTUS_LIFECYCLE_SUPERVISOR_TOKEN"
      )

      When("the Control Center resolves the declaration")
      val result = LifecycleSupervisorConfiguration.fromProperties(values)

      Then("only a loopback endpoint, bounded timeout, and environment-variable name are retained")
      result.map(_.map(_.supervisorId)) shouldBe Right(Some("local-cncf-launcher"))
      result.map(_.map(_.endpoint.toString)) shouldBe Right(Some("http://127.0.0.1:19400"))
      result.map(_.map(_.timeout)) shouldBe Right(Some(Duration.ofSeconds(2)))
      result.map(_.map(_.tokenEnv)) shouldBe Right(Some("TEXTUS_LIFECYCLE_SUPERVISOR_TOKEN"))
    }

    "reject incomplete, non-loopback, and unbounded declarations before dispatch" in {
      Given("unsafe lifecycle-supervisor declarations")
      val incomplete = Map(LifecycleSupervisorConfiguration.SUPERVISOR_ID -> "local")
      val remote = Map(
        LifecycleSupervisorConfiguration.SUPERVISOR_ID -> "local",
        LifecycleSupervisorConfiguration.ENDPOINT -> "http://example.test:19400",
        LifecycleSupervisorConfiguration.TOKEN_ENV -> "TEXTUS_LIFECYCLE_SUPERVISOR_TOKEN"
      )
      val unbounded = Map(
        LifecycleSupervisorConfiguration.SUPERVISOR_ID -> "local",
        LifecycleSupervisorConfiguration.ENDPOINT -> "http://localhost:19400",
        LifecycleSupervisorConfiguration.TIMEOUT -> "31s",
        LifecycleSupervisorConfiguration.TOKEN_ENV -> "TEXTUS_LIFECYCLE_SUPERVISOR_TOKEN"
      )
      val invalidtimeout = Map(
        LifecycleSupervisorConfiguration.SUPERVISOR_ID -> "local",
        LifecycleSupervisorConfiguration.ENDPOINT -> "http://localhost:19400",
        LifecycleSupervisorConfiguration.TIMEOUT -> "forever",
        LifecycleSupervisorConfiguration.TOKEN_ENV -> "TEXTUS_LIFECYCLE_SUPERVISOR_TOKEN"
      )

      When("the declarations are validated")
      val incompleteresult = LifecycleSupervisorConfiguration.fromProperties(incomplete)
      val remoteresult = LifecycleSupervisorConfiguration.fromProperties(remote)
      val unboundedresult = LifecycleSupervisorConfiguration.fromProperties(unbounded)
      val invalidtimeoutresult = LifecycleSupervisorConfiguration.fromProperties(invalidtimeout)

      Then("dispatch cannot silently use a remote endpoint or an unbounded timeout")
      incompleteresult.left.map(_.message) shouldBe Left("textus-control-center.lifecycle.supervisor.endpoint is required when lifecycle supervisor dispatch is configured")
      remoteresult.left.map(_.message) shouldBe Left("lifecycle supervisor endpoint must be an absolute loopback HTTP URI")
      unboundedresult.left.map(_.message) shouldBe Left("lifecycle supervisor timeout must not exceed 30 seconds")
      invalidtimeoutresult.left.map(_.message) shouldBe Left("lifecycle supervisor timeout is invalid")
    }
  }
}
