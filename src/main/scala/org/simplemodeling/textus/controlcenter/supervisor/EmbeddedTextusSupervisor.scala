/*
 *  version Jul. 24, 2026
 * @version Jul. 27, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.time.Instant

import org.simplemodeling.textus.controlcenter.launcher.LauncherLifecycleClient

trait TextusSupervisor {
  def submit(request: LifecycleSupervisorRequest, now: Instant): LifecycleSupervisorResult
  def lookup(requestId: String): Option[LifecycleSupervisorResult]
}

trait TextusSupervisorTransitionExecutor {
  def submit(request: LifecycleSupervisorRequest): Either[String, LifecycleSupervisorResult]
  def lookup(requestId: String): Option[LifecycleSupervisorResult]
}

final class LauncherLifecycleTransitionExecutor(
  client: LauncherLifecycleClient
) extends TextusSupervisorTransitionExecutor {
  def submit(request: LifecycleSupervisorRequest): Either[String, LifecycleSupervisorResult] =
    client.submit(request)

  def lookup(requestId: String): Option[LifecycleSupervisorResult] =
    client.lookup(requestId)
}

final class EmbeddedTextusSupervisor(
  executor: TextusSupervisorTransitionExecutor,
  supervisorId: String = EmbeddedTextusSupervisor.StandaloneSupervisorId
) extends TextusSupervisor {
  def submit(request: LifecycleSupervisorRequest, now: Instant): LifecycleSupervisorResult =
    executor.submit(request).fold(
      code => LifecycleSupervisorProtocol.unavailable(request, supervisorId, code, now),
      result => _project(request, result, now)
    )

  def lookup(requestId: String): Option[LifecycleSupervisorResult] =
    executor.lookup(requestId).filter(_.requestId == requestId).map(_project_lookup)

  private def _project(request: LifecycleSupervisorRequest, result: LifecycleSupervisorResult, now: Instant): LifecycleSupervisorResult =
    if (result.requestId == request.requestId) result.copy(supervisorId = supervisorId)
    else LifecycleSupervisorProtocol.unavailable(request, supervisorId, "textus-supervisor-transition-response-mismatch", now)

  private def _project_lookup(result: LifecycleSupervisorResult): LifecycleSupervisorResult =
    result.copy(supervisorId = supervisorId)
}

object EmbeddedTextusSupervisor {
  val StandaloneSupervisorId = "textus-supervisor-standalone"
}
