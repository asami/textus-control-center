/*
 * @version Jul. 27, 2026
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.time.Instant

import io.circe.syntax.*
import org.scalatest.GivenWhenThen
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class LauncherEvidenceSnapshotCodecSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "LauncherEvidenceSnapshotCodec" should {
    "round-trip launcher evidence through the versioned snapshot payload" in {
      Given("launcher evidence with optional artifact and stopped values")
      val entry = LauncherEvidenceEntry("cncf", "snapshot-instance", "snapshot-target", Some("snapshot-artifact"), "development", Some("Snapshot Subsystem"), Some("0.1.0"), "0.5.1", Instant.parse("2026-07-27T00:00:00Z"), Instant.parse("2026-07-27T00:00:30Z"), None)
      val observedat = Instant.parse("2026-07-27T00:01:00Z")

      When("the evidence is encoded and decoded")
      val decoded = LauncherEvidenceSnapshotCodec.decode(LauncherEvidenceSnapshotCodec.encode(entry, "current-evidence-only", observedat))

      Then("the snapshot retains the operational evidence facts")
      decoded.toOption.map { snapshot =>
        (snapshot.instanceId, snapshot.launcherKind, snapshot.artifactId, snapshot.observedAt, snapshot.evidenceDecision)
      } shouldBe Some(("snapshot-instance", "cncf", Some("snapshot-artifact"), "2026-07-27T00:01:00Z", "current-evidence-only"))
    }

    "reject malformed and unsupported snapshot payloads" in {
      Given("a well-formed snapshot with an unsupported schema")
      val unsupported = LauncherEvidenceSnapshotCodec.Snapshot("unsupported.v1", "snapshot-instance", "cncf", "snapshot-target", None, "development", None, None, "0.5.1", "2026-07-27T00:00:00Z", "2026-07-27T00:00:30Z", None, "2026-07-27T00:01:00Z", "current-evidence-only").asJson.noSpaces

      When("the codec receives malformed and unsupported payloads")
      val malformed = LauncherEvidenceSnapshotCodec.decode("{")
      val schemaMismatch = LauncherEvidenceSnapshotCodec.decode(unsupported)

      Then("both payloads are rejected")
      malformed.toOption shouldBe empty
      schemaMismatch.toOption shouldBe empty
    }
  }
}
