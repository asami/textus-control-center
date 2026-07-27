/*
 * @version Jul. 28, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.time.Instant

import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.supervisor.{Supervisor, SupervisorAction, SupervisorRequest, SupervisorResult, SupervisorState}

trait TextusSupervisor {
  def submit(request: LifecycleSupervisorRequest, now: Instant): LifecycleSupervisorResult
  def lookup(requestId: String): Option[LifecycleSupervisorResult]
}

final class EmbeddedTextusSupervisor(
  supervisor: Supervisor,
  executionContext: ExecutionContext,
  supervisorId: String = EmbeddedTextusSupervisor.StandaloneSupervisorId
) extends TextusSupervisor {
  def submit(request: LifecycleSupervisorRequest, now: Instant): LifecycleSupervisorResult = {
    given ExecutionContext = executionContext
    _request(request).fold(
      code => LifecycleSupervisorProtocol.unavailable(request, supervisorId, code, now),
      value =>
        supervisor.submit(value).toOption match {
          case Some(result) if result.requestId == request.requestId => _project(result)
          case Some(_) => LifecycleSupervisorProtocol.unavailable(request, supervisorId, "textus-supervisor-response-mismatch", now)
          case None => LifecycleSupervisorProtocol.unavailable(request, supervisorId, "textus-supervisor-unavailable", now)
        }
    )
  }

  def lookup(requestId: String): Option[LifecycleSupervisorResult] = {
    given ExecutionContext = executionContext
    supervisor.lookup(requestId).toOption.flatten.filter(_.requestId == requestId).map(_project)
  }

  private def _request(request: LifecycleSupervisorRequest): Either[String, SupervisorRequest] =
    _action(request.action).map(action =>
      SupervisorRequest(
        request.requestId,
        request.idempotencyKey,
        request.artifactId,
        Some("standalone"),
        action,
        request.operatorSubjectId,
        request.deadlineAt
      )
    )

  private def _action(action: String): Either[String, SupervisorAction] =
    action match {
      case "start" => Right(SupervisorAction.Start)
      case "stop" => Right(SupervisorAction.Stop)
      case "restart" => Right(SupervisorAction.Restart)
      case _ => Left("textus-supervisor-action-invalid")
    }

  private def _project(result: SupervisorResult): LifecycleSupervisorResult =
    LifecycleSupervisorResult(
      result.requestId,
      _state(result.state),
      result.diagnosticCode,
      result.diagnostic,
      result.supervisorId,
      result.instanceId,
      result.acceptedAt,
      result.completedAt
    )

  private def _state(state: SupervisorState): String =
    state match {
      case SupervisorState.Queued => "queued"
      case SupervisorState.Accepted => "accepted"
      case SupervisorState.Running => "running"
      case SupervisorState.Stopped => "stopped"
      case SupervisorState.Rejected => "rejected"
      case SupervisorState.Failed => "failed"
      case SupervisorState.TimedOut => "timed-out"
    }
}

object EmbeddedTextusSupervisor {
  val StandaloneSupervisorId = "textus-supervisor-standalone"
}
