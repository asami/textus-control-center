/*
 * @version Jul. 19, 2026
 */
package org.simplemodeling.textus.controlcenter.registry

import java.time.{Duration, Instant}

import org.scalacheck.{Gen, Prop, Test as ScalaCheckTest}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SubsystemRegistrySpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Subsystem registry" should {
    "register an invocation idempotently" in {
      Given("a valid starting report and its launcher principal")
      val input = _input(launcherState = SubsystemRegistry.starting)
      val received = Instant.parse("2026-07-18T00:00:00Z")

      When("the launcher registers the same invocation twice")
      val first = SubsystemRegistry.register(None, input, "launcher-a", received)
      val second = first.flatMap(current =>
        SubsystemRegistry.register(Some(current), input, "launcher-a", received.plusSeconds(30))
      )

      Then("both reports are accepted and the receipt time is refreshed")
      first.map(_.lastSeenAt) shouldBe Right(received)
      second.map(_.lastSeenAt) shouldBe Right(received.plusSeconds(30))
    }

    "reject a heartbeat from a different registration principal" in {
      Given("a registered Textus launcher instance")
      val input = _input(launcherState = SubsystemRegistry.starting)
      val current = SubsystemRegistry.register(
        None,
        input,
        "launcher-a",
        Instant.parse("2026-07-18T00:00:00Z")
      ).toOption

      When("another principal sends a heartbeat")
      val unauthorized = SubsystemRegistry.heartbeat(
        current,
        _input(launcherState = SubsystemRegistry.running),
        "launcher-b",
        Instant.parse("2026-07-18T00:00:30Z")
      )

      Then("the registry rejects the report without changing the instance")
      unauthorized.left.toOption shouldBe Some(RegistryError.Unauthorized("instance-1"))
    }

    "reject a heartbeat with conflicting immutable identity" in {
      Given("a registered Textus launcher instance")
      val input = _input(launcherState = SubsystemRegistry.starting)
      val current = SubsystemRegistry.register(
        None,
        input,
        "launcher-a",
        Instant.parse("2026-07-18T00:00:00Z")
      ).toOption

      When("the owner changes the target in a heartbeat")
      val conflict = SubsystemRegistry.heartbeat(
        current,
        _input(launcherState = SubsystemRegistry.running, target = "other-target"),
        "launcher-a",
        Instant.parse("2026-07-18T00:00:30Z")
      )

      Then("the registry returns a structured conflict")
      conflict.left.toOption shouldBe Some(RegistryError.Conflict("instance-1", "target differs"))
    }

    "expose execution mode and development directory in the administrative projection" in {
      Given("a development-directory launched subsystem")
      val instance = _registered(lastSeenAt = Instant.parse("2026-07-18T00:00:00Z"))

      When("an operator reads its projection")
      val projection = SubsystemRegistry.projection(instance, instance.lastSeenAt, Duration.ofSeconds(90))

      Then("the component identity and local development location remain available")
      projection.map(_.executionMode) shouldBe Right(Some("development"))
      projection.map(_.developmentDirectory) shouldBe Right(Some("/work/textus-control-center"))
    }

    "reject a missing or stopped instance heartbeat" in {
      Given("a valid running heartbeat report")
      val input = _input(launcherState = SubsystemRegistry.running)
      val time = Instant.parse("2026-07-18T00:00:00Z")
      val stopped = _registered(lastSeenAt = time).copy(launcherState = SubsystemRegistry.stopped)

      When("the launcher reports a missing or stopped invocation")
      val missing = SubsystemRegistry.heartbeat(None, input, "launcher-a", time)
      val stoppedresult = SubsystemRegistry.heartbeat(Some(stopped), input, "launcher-a", time)

      Then("neither report can revive an unregistered or stopped record")
      missing.left.toOption shouldBe Some(RegistryError.Missing("instance-1"))
      stoppedresult.left.toOption shouldBe Some(
        RegistryError.Conflict("instance-1", "the instance has already stopped")
      )
    }

    "derive stale exactly at the configured threshold" in {
      Given("a fresh running instance with a ten-second lease")
      val instance = _registered(lastSeenAt = Instant.parse("2026-07-18T00:00:00Z"))
      val threshold = Duration.ofSeconds(10)

      When("an operator reads immediately before and exactly at expiry")
      val fresh = SubsystemRegistry.projection(instance, instance.lastSeenAt.plusSeconds(9), threshold)
      val expired = SubsystemRegistry.projection(instance, instance.lastSeenAt.plusSeconds(10), threshold)

      Then("the state changes from running to stale at the boundary")
      fresh.map(_.status) shouldBe Right(SubsystemRegistry.running)
      expired.map(_.status) shouldBe Right(SubsystemRegistry.stale)
    }

    "preserve the stale boundary for every positive whole-second threshold" in {
      Given("a running instance reported at a fixed receipt time")
      val instance = _registered(lastSeenAt = Instant.parse("2026-07-18T00:00:00Z"))

      When("the registry evaluates generated positive lease durations")
      val property = Prop.forAll(Gen.chooseNum(1, 3600)) { seconds =>
        val threshold = Duration.ofSeconds(seconds.toLong)
        val before = SubsystemRegistry.projection(instance, instance.lastSeenAt.plusSeconds(seconds - 1L), threshold)
        val at = SubsystemRegistry.projection(instance, instance.lastSeenAt.plusSeconds(seconds), threshold)
        before.map(_.status).contains(SubsystemRegistry.running) &&
          at.map(_.status).contains(SubsystemRegistry.stale)
      }
      val result = ScalaCheckTest.check(ScalaCheckTest.Parameters.default, property)

      Then("every generated lease remains fresh before expiry and stale at expiry")
      result.passed shouldBe true
    }

    "retain a stopped instance and order instances deterministically" in {
      Given("two instances with the same receipt time")
      val time = Instant.parse("2026-07-18T00:00:00Z")
      val alpha = _registered(instanceId = "a", lastSeenAt = time)
      val beta = _registered(instanceId = "b", lastSeenAt = time)

      When("the second launcher deregisters and an operator lists both records")
      val stopped = SubsystemRegistry.deregister(Some(beta), "b", "launcher-a", time.plusSeconds(1))
      val ordered = SubsystemRegistry.ordered(Vector(alpha, stopped.toOption.get))

      Then("the stopped record remains and latest receipt time sorts first")
      stopped.map(_.launcherState) shouldBe Right(SubsystemRegistry.stopped)
      ordered.map(_.instanceId) shouldBe Vector("b", "a")
    }
  }

  private def _input(
    launcherState: String,
    target: String = "textus-control-center"
  ): RegistrationInput =
    RegistrationInput(
      protocolVersion = SubsystemRegistry.protocolVersion,
      instanceId = "instance-1",
      launcherKind = "textus",
      target = target,
      executionMode = Some("development"),
      developmentDirectory = Some("/work/textus-control-center"),
      subsystemName = Some("Textus Control Center"),
      subsystemVersion = Some("0.1.0-SNAPSHOT"),
      runtimeVersion = Some("0.5.0"),
      baseUrl = "https://admin.example.test",
      hostLabel = "test",
      startedAt = Instant.parse("2026-07-18T00:00:00Z"),
      launcherState = launcherState
    )

  private def _registered(
    instanceId: String = "instance-1",
    lastSeenAt: Instant
  ): RegisteredSubsystem =
    RegisteredSubsystem(
      protocolVersion = SubsystemRegistry.protocolVersion,
      instanceId = instanceId,
      launcherKind = "textus",
      target = "textus-control-center",
      executionMode = Some("development"),
      developmentDirectory = Some("/work/textus-control-center"),
      subsystemName = Some("Textus Control Center"),
      subsystemVersion = Some("0.1.0-SNAPSHOT"),
      runtimeVersion = Some("0.5.0"),
      baseUrl = "https://admin.example.test",
      hostLabel = "test",
      startedAt = lastSeenAt,
      lastSeenAt = lastSeenAt,
      launcherState = SubsystemRegistry.running,
      registrationPrincipalId = "launcher-a"
    )
}
