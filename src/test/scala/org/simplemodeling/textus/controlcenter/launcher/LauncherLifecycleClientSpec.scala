/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.time.Instant

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.controlcenter.supervisor.LifecycleSupervisorRequest

final class LauncherLifecycleClientSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Launcher lifecycle client" should {
    "invoke only the bounded Launcher lifecycle commands without a supervisor endpoint or credential" in {
      Given("a persisted lifecycle request and a bounded Launcher command runner")
      var calls = Vector.empty[Vector[String]]
      val runner = new LauncherLifecycleCommandRunner {
        def run(configuration: LauncherLifecycleClientConfiguration, args: Vector[String]) = {
          calls :+= args
          Right("""{"requestId":"request-1","state":"accepted","diagnosticCode":null,"diagnostic":null,"supervisorId":"local-supervisor","instanceId":"instance-1","acceptedAt":"2026-07-22T00:00:00Z","completedAt":null}""")
        }
      }
      val request = LifecycleSupervisorRequest("request-1", "key-1", "textus-control-center", "start", "operator-1", Instant.parse("2026-07-22T00:00:05Z"))
      val client = LauncherLifecycleClient(LauncherLifecycleClientConfiguration("cncf", java.time.Duration.ofSeconds(5)), runner)

      When("the client submits and looks up the same request")
      val submitted = client.submit(request)
      val lookedup = client.lookup("request-1")

      Then("it forwards only the fixed Launcher command arguments and safe result")
      submitted.map(_.supervisorId) shouldBe Right("local-supervisor")
      lookedup.map(_.instanceId) shouldBe Some(Some("instance-1"))
      calls shouldBe Vector(
        Vector("launcher", "lifecycle", "submit", "--request-id", "request-1", "--idempotency-key", "key-1", "--artifact-id", "textus-control-center", "--action", "start", "--operator-subject-id", "operator-1", "--deadline-at", "2026-07-22T00:00:05Z"),
        Vector("launcher", "lifecycle", "lookup", "request-1")
      )
    }

    "reserve enough timeout for authority cold-start and lifecycle submission" in {
      Given("the bounded Control Center lifecycle adapter configuration")
      val default = LauncherLifecycleClientConfiguration.fromProperties(Map.empty)
      val short = LauncherLifecycleClientConfiguration.fromProperties(Map(
        LauncherLifecycleClientConfiguration.Timeout -> "5s"
      ))
      val bounded = LauncherLifecycleClientConfiguration.fromProperties(Map(
        LauncherLifecycleClientConfiguration.Timeout -> "20s"
      ))

      When("the default, a too-short value, and the minimum bounded value are resolved")

      Then("only a timeout that covers cold-start and submission is accepted")
      default.map(_.timeout) shouldBe Right(java.time.Duration.ofSeconds(20))
      short shouldBe Left("launcher-lifecycle-timeout-invalid")
      bounded.map(_.timeout) shouldBe Right(java.time.Duration.ofSeconds(20))
    }
  }
}
