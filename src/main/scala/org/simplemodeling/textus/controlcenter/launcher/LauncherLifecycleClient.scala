/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.time.Duration

import org.simplemodeling.textus.controlcenter.supervisor.{LifecycleSupervisorProtocol, LifecycleSupervisorRequest, LifecycleSupervisorResult}

final case class LauncherLifecycleClientConfiguration(command: String, timeout: Duration)

object LauncherLifecycleClientConfiguration {
  val Command = "textus-control-center.launcher.lifecycle.command"
  val Timeout = "textus-control-center.launcher.lifecycle.timeout"
  val DefaultCommand = "cncf"
  val MinimumTimeout = Duration.ofSeconds(20)
  val DefaultTimeout = MinimumTimeout
  val MaximumTimeout = Duration.ofSeconds(30)

  def fromProperties(properties: Map[String, String]): Either[String, LauncherLifecycleClientConfiguration] =
    for {
      command <- properties.get(Command).map(_.trim).filter(_.nonEmpty).getOrElse(DefaultCommand).split("\\s+").toVector match {
        case Vector(value) if !value.contains("\u0000") => Right(value)
        case _ => Left("launcher-lifecycle-command-invalid")
      }
      timeout <- properties.get(Timeout).map(_.trim).filter(_.nonEmpty).fold[Either[String, Duration]](Right(DefaultTimeout))(_timeout)
    } yield LauncherLifecycleClientConfiguration(command, timeout)

  private def _timeout(value: String): Either[String, Duration] = {
    val parsed =
      if (value.matches("[0-9]+ms")) scala.util.Try(Duration.ofMillis(value.dropRight(2).toLong)).toOption
      else scala.util.Try(Duration.parse(if (value.matches("[0-9]+s")) s"PT${value.dropRight(1)}S" else value)).toOption
    parsed.filter(timeout => timeout.compareTo(MinimumTimeout) >= 0 && timeout.compareTo(MaximumTimeout) <= 0)
      .toRight("launcher-lifecycle-timeout-invalid")
  }
}

trait LauncherLifecycleCommandRunner {
  def run(configuration: LauncherLifecycleClientConfiguration, args: Vector[String]): Either[String, String]
}

object LauncherLifecycleCommandRunner {
  object System extends LauncherLifecycleCommandRunner {
    def run(configuration: LauncherLifecycleClientConfiguration, args: Vector[String]): Either[String, String] =
      LauncherEvidenceCommandRunner.System.run(
        LauncherEvidenceClientConfiguration(configuration.command, configuration.timeout),
        args
      ).left.map {
        case "launcher-evidence-timeout" => "launcher-lifecycle-timeout"
        case "launcher-evidence-output-too-large" => "launcher-lifecycle-output-too-large"
        case "launcher-evidence-command-failed" => "launcher-lifecycle-command-failed"
        case _ => "launcher-lifecycle-command-unavailable"
      }
  }
}

final class LauncherLifecycleClient(
  configuration: LauncherLifecycleClientConfiguration,
  runner: LauncherLifecycleCommandRunner = LauncherLifecycleCommandRunner.System
) {
  def submit(request: LifecycleSupervisorRequest): Either[String, LifecycleSupervisorResult] =
    runner.run(configuration, Vector(
      "launcher", "lifecycle", "submit",
      "--request-id", request.requestId,
      "--idempotency-key", request.idempotencyKey,
      "--artifact-id", request.artifactId,
      "--action", request.action,
      "--operator-subject-id", request.operatorSubjectId,
      "--deadline-at", request.deadlineAt.toString
    )).flatMap(LifecycleSupervisorProtocol.response(_, request.requestId))

  def lookup(requestid: String): Option[LifecycleSupervisorResult] =
    runner.run(configuration, Vector("launcher", "lifecycle", "lookup", requestid))
      .flatMap(LifecycleSupervisorProtocol.response(_, requestid)).toOption
}
