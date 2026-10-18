/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
package org.simplemodeling.textus.controlcenter.summary

import java.time.Instant
import org.goldenport.cncf.component.Component
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.registry.RegisteredSubsystem
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class OperationalSummarySpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Operational Summary projection" should {
    "bound compact data while counting every latest instance in registry order" in {
      Given("generated registry sizes and admitted compact limits, with older duplicate reports")
      val property = Prop.forAll(Gen.choose(1, 150), Gen.choose(1, 100)) { (size, bound) =>
        val sources = Vector.tabulate(size) { index =>
          OperationalSummary.StoredInstance(s"new-$index", _registered(s"instance-$index", _now.minusSeconds(index.toLong)))
        }
        val older = sources.map(source => source.copy(recordId = s"old-${source.recordId}", value = source.value.copy(lastSeenAt = _now.minusSeconds(1000))))
        val result = OperationalSummary.createLimitC(Some(bound)).flatMap(limit =>
          OperationalSummary.runtimes((sources ++ older).reverse, _now, limit)
        ).toOption
        result.exists { record =>
          val data = _records(record)
          val counts = Vector("starting", "running", "stopped", "stale").flatMap(record.getInt).sum
          record.getInt("total").contains(size) &&
            record.getInt("returned").contains(size.min(bound)) &&
            record.getAny("truncated").contains(size > bound) &&
            counts == size &&
            data.flatMap(_.getString("instanceId")) == sources.take(bound).map(_.value.instanceId)
        }
      }

      When("the shared production projection selects latest reports, counts, orders, and bounds them")
      val checked = Test.check(Test.Parameters.default, property)

      Then("counts include unreturned instances and compact data follows deterministic registry ordering")
      checked.passed shouldBe true
    }

    "break latest-report ties by the printed entity id independently of input order" in {
      Given("generated instance identities with equal receipt times and different stored ids")
      val property = Prop.forAll(Gen.choose(1, 10000)) { index =>
        val source = _registered(s"instance-$index", _now)
        val first = OperationalSummary.StoredInstance("a", source.copy(target = "older-id"))
        val last = OperationalSummary.StoredInstance("z", source.copy(target = "selected-id"))
        val limit = OperationalSummary.createLimitC(None).toOption.get
        val forward = OperationalSummary.runtimes(Vector(first, last), _now, limit).toOption
        val reverse = OperationalSummary.runtimes(Vector(last, first), _now, limit).toOption
        Vector(forward, reverse).forall(_.exists(record =>
          record.getInt("total").contains(1) && _records(record).head.getString("target").contains("selected-id")
        ))
      }

      When("the production projection receives each permutation")
      val checked = Test.check(Test.Parameters.default, property)

      Then("the greatest stored id wins each tie without double counting")
      checked.passed shouldBe true
    }

    "keep lease state distinct from unknown runtime health" in {
      Given("generated fresh running, starting, stopped, and expired running leases")
      val property = Prop.forAll(Gen.choose(0L, 89L), Gen.choose(90L, 10000L)) { (freshage, staleage) =>
        val sources = Vector(
          _registered("running", _now.minusSeconds(freshage)),
          _registered("starting", _now).copy(launcherState = "starting"),
          _registered("stopped", _now.minusSeconds(staleage)).copy(launcherState = "stopped"),
          _registered("stale", _now.minusSeconds(staleage))
        ).map(source => OperationalSummary.StoredInstance(source.instanceId, source))
        val limit = OperationalSummary.createLimitC(None).toOption.get
        OperationalSummary.runtimes(sources, _now, limit).toOption.exists { record =>
          record.getString("health").contains("unavailable") &&
            Vector("starting", "running", "stopped", "stale").forall(name => record.getInt(name).contains(1)) &&
            _records(record).map(entry => entry.getString("instanceId") -> entry.getString("status")).toMap ==
              Vector("running", "starting", "stopped", "stale").map(name => Some(name) -> Some(name)).toMap
        }
      }

      When("the production projection applies the existing ninety-second lease semantics")
      val checked = Test.check(Test.Parameters.default, property)

      Then("fresh running remains a lease observation and remote health stays unavailable")
      checked.passed shouldBe true
    }

    "exclude private registration fields from compact records" in {
      Given("generated private principal, path, host, and endpoint values in registry reports")
      val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)) { secret =>
        val source = _registered("safe-instance", _now).copy(
          registrationPrincipalId = s"principal-$secret",
          developmentDirectory = Some(s"/private/$secret"),
          hostLabel = s"host-$secret",
          baseUrl = s"https://$secret.example.test",
          applicationUrl = Some(s"https://$secret.example.test/web/private")
        )
        val limit = OperationalSummary.createLimitC(None).toOption.get
        OperationalSummary.runtimes(Vector(OperationalSummary.StoredInstance("stored", source)), _now, limit).toOption.exists { record =>
          val entry = _records(record).head
          Vector("registrationPrincipalId", "developmentDirectory", "executionMode", "hostLabel", "baseUrl", "applicationUrl", "diagnostic", "credentials")
            .forall(name => entry.getAny(name).isEmpty) &&
            entry.getString("artifactId").contains("safe-artifact") &&
            entry.getString("subsystemVersion").contains("1.2.3")
        }
      }

      When("the production projection creates compact runtime entries")
      val checked = Test.check(Test.Parameters.default, property)

      Then("only the admitted compact identity, version, and lease fields are exposed")
      checked.passed shouldBe true
    }

    "derive nonnegative integral uptime from actual boot time under elapsed time and clock skew" in {
      Given("generated elapsed seconds on either side of a known boot instant")
      val property = Prop.forAll(Gen.choose(-100000L, 100000L), Gen.choose(0L, 999999999L)) { (elapsed, nanos) =>
        val now = _now.plusSeconds(elapsed).plusNanos(nanos)
        val result = OperationalSummary.uptime(Some(_now), now)
        result.getString("state").contains("available") &&
          result.getAny("seconds").contains(elapsed.max(0L))
      }

      When("the production uptime projection compares the captured request instant with bootedAt")
      val checked = Test.check(Test.Parameters.default, property)

      Then("uptime is integral elapsed seconds clamped at zero for backward clock skew")
      checked.passed shouldBe true
    }

    "represent missing runtime context and unknown component version explicitly" in {
      Given("a genuine component identity and health snapshot without runtime metadata or a known version")
      val limit = OperationalSummary.createLimitC(None).toOption.get
      val health = Component.HealthSnapshot("warning", Vector(Component.HealthCheck("safe-check", "warning", Some("private diagnostic"))))

      When("the shared projection creates the Summary at its supplied request instant")
      val result = OperationalSummary.project("component-id", "instance-id", Some("unknown"), None, health, Vector.empty, _now, limit)
        .toOption.getOrElse(fail("summary projection failed"))

      Then("metadata is unavailable without inventing zero uptime and health contains only safe fields")
      val controlcenter = _record(result, "controlCenter")
      _record(controlcenter, "version").getString("state") shouldBe Some("unavailable")
      _record(controlcenter, "version").getAny("value") shouldBe empty
      _record(controlcenter, "uptime").getString("state") shouldBe Some("unavailable")
      _record(controlcenter, "uptime").getAny("seconds") shouldBe empty
      _record(controlcenter, "runtimeVersion").getString("state") shouldBe Some("unavailable")
      result.getString("observedAt") shouldBe Some(_now.toString)
      val overall = _record(result, "overallHealth")
      overall.getString("status") shouldBe Some("warning")
      overall.getString("scope") shouldBe Some("control-center")
      _records(overall, "checks").head.getAny("detail") shouldBe empty
      result.getString("dashboardUrl") shouldBe Some("/web/textus-control-center")
      result.getInt("refreshAfterSeconds") shouldBe Some(15)
    }

    "keep missing runtime metadata unavailable for every supplied request clock" in {
      Given("generated request instants without a GlobalRuntimeContext source")
      val property = Prop.forAll(Gen.choose(-1000000000L, 2000000000L)) { epochsecond =>
        val now = Instant.ofEpochSecond(epochsecond)
        val limit = OperationalSummary.createLimitC(None).toOption.get
        OperationalSummary.project("actual-component", "actual-instance", None, None,
          Component.HealthSnapshot("ok", Vector.empty), Vector.empty, now, limit).toOption.exists { summary =>
          val controlcenter = _record(summary, "controlCenter")
          val uptime = _record(controlcenter, "uptime")
          val runtimeversion = _record(controlcenter, "runtimeVersion")
          summary.getString("observedAt").contains(now.toString) &&
            uptime.getString("state").contains("unavailable") && uptime.getAny("seconds").isEmpty &&
            runtimeversion.getString("state").contains("unavailable") && runtimeversion.getAny("value").isEmpty
        }
      }

      When("the production Summary projection consumes the absent runtime source")
      val checked = Test.check(Test.Parameters.default, property)

      Then("request time never becomes a fabricated boot time, zero uptime, or runtime version")
      checked.passed shouldBe true
    }

    "reject every out-of-range bound and supply the documented default" in {
      Given("generated bounds outside one through one hundred, and an absent bound")
      val property = Prop.forAll(Gen.oneOf(Gen.choose(Int.MinValue, 0), Gen.choose(101, Int.MaxValue))) { bound =>
        OperationalSummary.createLimitC(Some(bound)).toOption.isEmpty
      }

      When("the production limit constructor admits the request")
      val checked = Test.check(Test.Parameters.default, property)
      val default = OperationalSummary.createLimitC(None).toOption

      Then("invalid bounds fail and the absent bound admits a fifty-entry projection")
      checked.passed shouldBe true
      default shouldBe OperationalSummary.createLimitC(Some(50)).toOption
    }
  }

  private val _now = Instant.parse("2026-10-18T00:00:00Z")

  private def _registered(instanceid: String, lastseenat: Instant): RegisteredSubsystem =
    RegisteredSubsystem(1, instanceid, "textus", "safe-target", Some("safe-artifact"), Some("development"),
      Some("/private/work"), Some("SafeSubsystem"), Some("1.2.3"), Some("4.5.6"),
      "https://private.example.test", "private-host", _now.minusSeconds(1000), lastseenat,
      "running", "private-principal", Some("https://private.example.test/web/private"))

  private def _record(record: Record, name: String): Record =
    record.getAny(name).collect { case value: Record => value }.getOrElse(fail(s"$name record missing"))

  private def _records(record: Record, name: String = "data"): Vector[Record] =
    record.getAny(name).collect { case values: Seq[?] => values.collect { case value: Record => value }.toVector }
      .getOrElse(Vector.empty)
}
