/*
 * @since   Jul. 22, 2026
 * @version Jul. 22, 2026
 * @author  ASAMI, Tomoharu
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.io.{ByteArrayOutputStream, InputStream}
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

final case class LauncherEvidenceClientConfiguration(command: String, timeout: Duration)

object LauncherEvidenceClientConfiguration {
  val Command = "textus-control-center.launcher.evidence.command"
  val Timeout = "textus-control-center.launcher.evidence.timeout"
  val DefaultCommand = "cncf"
  val DefaultTimeout = Duration.ofSeconds(5)
  val MaximumTimeout = Duration.ofSeconds(30)

  def fromProperties(properties: Map[String, String]): Either[String, LauncherEvidenceClientConfiguration] =
    for {
      command <- properties.get(Command).map(_.trim).filter(_.nonEmpty).getOrElse(DefaultCommand).split("\\s+").toVector match {
        case Vector(value) if !value.contains("\u0000") => Right(value)
        case _ => Left("launcher-evidence-command-invalid")
      }
      timeout <- properties.get(Timeout).map(_.trim).filter(_.nonEmpty).fold[Either[String, Duration]](Right(DefaultTimeout))(_timeout)
    } yield LauncherEvidenceClientConfiguration(command, timeout)

  private def _timeout(value: String): Either[String, Duration] = {
    val parsed =
      if (value.matches("[0-9]+ms")) scala.util.Try(Duration.ofMillis(value.dropRight(2).toLong)).toOption
      else scala.util.Try(Duration.parse(if (value.matches("[0-9]+s")) s"PT${value.dropRight(1)}S" else value)).toOption
    parsed.filter(timeout => !timeout.isZero && !timeout.isNegative && timeout.compareTo(MaximumTimeout) <= 0)
      .toRight("launcher-evidence-timeout-invalid")
  }
}

trait LauncherEvidenceCommandRunner {
  def run(configuration: LauncherEvidenceClientConfiguration, args: Vector[String]): Either[String, String]
}

object LauncherEvidenceCommandRunner {
  object System extends LauncherEvidenceCommandRunner {
    def run(configuration: LauncherEvidenceClientConfiguration, args: Vector[String]): Either[String, String] =
      try {
        val process = ProcessBuilder((configuration.command +: args)*).redirectErrorStream(true).start()
        val executor = Executors.newSingleThreadExecutor()
        val output = executor.submit(() => _read(process.getInputStream))
        try {
          if (!process.waitFor(configuration.timeout.toMillis, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            Left("launcher-evidence-timeout")
          } else if (process.exitValue() != 0) {
            Left("launcher-evidence-command-failed")
          } else {
            output.get(1, TimeUnit.SECONDS)
          }
        } finally {
          executor.shutdownNow()
        }
      } catch {
        case _: Throwable => Left("launcher-evidence-command-unavailable")
      }

    private def _read(stream: InputStream): Either[String, String] = {
      val buffer = new Array[Byte](8192)
      val output = ByteArrayOutputStream()
      var total = 0
      var count = stream.read(buffer)
      while (count >= 0) {
        total += count
        if (total > 1024 * 1024) return Left("launcher-evidence-output-too-large")
        output.write(buffer, 0, count)
        count = stream.read(buffer)
      }
      Right(new String(output.toByteArray, StandardCharsets.UTF_8))
    }
  }
}

final class LauncherEvidenceClient(
  configuration: LauncherEvidenceClientConfiguration,
  runner: LauncherEvidenceCommandRunner = LauncherEvidenceCommandRunner.System
) {
  def list(): Either[String, LauncherEvidenceListProjection] =
    runner.run(configuration, Vector("launcher", "evidence", "list", "--format", "json")).flatMap(LauncherEvidenceProtocol.list)

  def detail(instanceid: String): Either[String, LauncherEvidenceDetailProjection] =
    runner.run(configuration, Vector("launcher", "evidence", "show", instanceid, "--format", "json")).flatMap(LauncherEvidenceProtocol.detail(_, instanceid))
}
