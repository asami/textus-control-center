/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.launcher

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class LauncherEvidenceProtocolSpec extends AnyWordSpec with Matchers {
  "Launcher evidence protocol" should {
    "accept the versioned list projection without a development directory" in {
      val body =
        """{"schema":"cncf.launcher.evidence-projection.v1","entries":[{"launcherKind":"textus","instanceId":"instance-1","target":"textus-control-center","artifactId":"textus-control-center","executionMode":"development","subsystemName":"Textus Control Center","subsystemVersion":"0.1.0-SNAPSHOT","runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":null}]}"""

      val projection = LauncherEvidenceProtocol.list(body)

      projection.map(_.entries.map(_.instanceId)) shouldBe Right(Vector("instance-1"))
      projection.map(_.entries.head.productElementNames.toVector.contains("developmentDirectory")) shouldBe Right(false)
    }

    "reject a detail response for another evidence identity" in {
      val body =
        """{"schema":"cncf.launcher.evidence-projection.v1","entry":{"launcherKind":"cncf","instanceId":"other-instance","target":"sample","artifactId":null,"executionMode":"development","developmentDirectory":"/private/work/sample","subsystemName":null,"subsystemVersion":null,"runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":null}}"""

      LauncherEvidenceProtocol.detail(body, "instance-1") shouldBe Left(LauncherEvidenceProtocol.InvalidProjection)
    }

    "invoke only the fixed Launcher evidence commands" in {
      val runner = FakeRunner(
        """{"schema":"cncf.launcher.evidence-projection.v1","entries":[]}""",
        """{"schema":"cncf.launcher.evidence-projection.v1","entry":{"launcherKind":"cncf","instanceId":"instance-1","target":"sample","artifactId":null,"executionMode":"development","developmentDirectory":"/private/work/sample","subsystemName":null,"subsystemVersion":null,"runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":null}}"""
      )
      val client = LauncherEvidenceClient(LauncherEvidenceClientConfiguration("cncf", java.time.Duration.ofSeconds(2)), runner)

      client.list().map(_.entries) shouldBe Right(Vector.empty)
      client.detail("instance-1").map(_.entry.developmentDirectory) shouldBe Right(Some("/private/work/sample"))
      runner.arguments shouldBe Vector(
        Vector("launcher", "evidence", "list", "--format", "json"),
        Vector("launcher", "evidence", "show", "instance-1", "--format", "json")
      )
    }
  }

  private final class FakeRunner(listbody: String, detailbody: String) extends LauncherEvidenceCommandRunner {
    var arguments: Vector[Vector[String]] = Vector.empty

    def run(configuration: LauncherEvidenceClientConfiguration, args: Vector[String]): Either[String, String] = {
      arguments :+= args
      if (args.contains("list")) Right(listbody) else Right(detailbody)
    }
  }
}
