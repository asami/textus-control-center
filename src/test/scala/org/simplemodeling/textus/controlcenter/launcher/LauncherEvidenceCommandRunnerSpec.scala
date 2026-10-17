/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Duration
import org.scalacheck.{Gen, Prop, Test as ScalaCheckTest}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class LauncherEvidenceCommandRunnerSpec
    extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _empty_list = """{"schema":"cncf.launcher.evidence-projection.v1","entries":[]}"""
  private val _valid_stdout = s"""printf '%s\n' '${_empty_list}'"""

  "Launcher evidence command transport" should {
    "interpret only stdout as the machine projection" which {
      "accepts valid stdout for at least 50 generated diagnostic warnings" in {
        val warnings = Gen.choose(1, 128).flatMap(length => Gen.listOfN(length, Gen.alphaNumChar)).map(_.mkString)
        val property = Prop.forAll(warnings) { warning =>
          Given("an executable Launcher fixture with valid stdout and an alphanumeric diagnostic on stderr")
          val script = s"""printf '%s\n' '$warning' >&2
${_valid_stdout}
"""
          _with_command(script) { client =>
            When("the client requests the fixed Launcher evidence list projection")
            val result = client.list()

            Then("the diagnostic leaves the empty versioned list intact")
            result.map(_.entries) shouldBe Right(Vector.empty)
          }
          true
        }
        val result = ScalaCheckTest.check(ScalaCheckTest.Parameters.default.withMinSuccessfulTests(50), property)

        Then("at least 50 generated diagnostics preserve the stdout projection without discarded cases")
        result.passed shouldBe true
        result.succeeded should be >= 50
        result.discarded shouldBe 0
      }

      "accepts valid stdout after diagnostic output exceeds a normal pipe buffer" in {
        Given("an executable Launcher fixture writing 256 diagnostic lines of 1024 bytes before valid stdout")
        val diagnostic = "D" * 1024
        val script = s"""i=0
while [ "$$i" -lt 256 ]; do
  printf '%s\n' '$diagnostic' >&2
  i=$$((i + 1))
done
${_valid_stdout}
"""
        _with_command(script) { client =>
          When("the client requests the list within its two-second command bound")
          val result = client.list()

          Then("discarded diagnostics cause neither projection corruption nor a backpressure timeout")
          result.map(_.entries) shouldBe Right(Vector.empty)
        }
      }

      "rejects invalid stdout even when stderr contains a valid projection" in {
        Given("an executable Launcher fixture with invalid stdout and valid versioned JSON only on stderr")
        val script = s"""printf '%s\n' '${_empty_list}' >&2
printf '%s\n' 'invalid stdout'
"""
        _with_command(script) { client =>
          When("the client requests the evidence list")
          val result = client.list()

          Then("invalid stdout remains a structured projection failure without a stderr fallback")
          result shouldBe Left(LauncherEvidenceProtocol.InvalidProjection)
        }
      }

      "preserves command failure for a nonzero exit despite valid stdout" in {
        Given("an executable Launcher fixture with valid stdout, a diagnostic, and exit status 23")
        val script = s"""${_valid_stdout}
printf '%s\n' 'diagnostic' >&2
exit 23
"""
        _with_command(script) { client =>
          When("the client requests the evidence list")
          val result = client.list()

          Then("the nonzero exit remains a structured command failure")
          result shouldBe Left("launcher-evidence-command-failed")
        }
      }
    }
  }

  private def _with_command(script: String)(check: LauncherEvidenceClient => Unit): Unit = {
    val parent = Files.createDirectories(Paths.get("target", "launcher-evidence-process-spec"))
    val directory = Files.createTempDirectory(parent, "command-")
    val command = directory.resolve("launcher-fixture.sh")
    try {
      val header = """#!/bin/sh
set -eu
if [ "$*" != "launcher evidence list --format json" ]; then exit 64; fi
"""
      Files.writeString(command, header + script, StandardCharsets.UTF_8)
      command.toFile.setExecutable(true) shouldBe true
      val configuration = LauncherEvidenceClientConfiguration(command.toAbsolutePath.toString, Duration.ofSeconds(2))
      val client = LauncherEvidenceClient(configuration)
      check(client)
    } finally {
      try Files.deleteIfExists(command)
      finally Files.deleteIfExists(directory)
    }
  }
}
