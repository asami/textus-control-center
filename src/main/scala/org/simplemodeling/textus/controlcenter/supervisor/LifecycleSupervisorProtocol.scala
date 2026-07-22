/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.net.URI
import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.parser.decode
import io.circe.syntax.*

final case class LifecycleSupervisorRequest(
  requestId: String,
  idempotencyKey: String,
  artifactId: String,
  action: String,
  operatorSubjectId: String,
  deadlineAt: Instant
)

final case class LifecycleSupervisorResult(
  requestId: String,
  state: String,
  diagnosticCode: Option[String],
  diagnostic: Option[String],
  supervisorId: String,
  instanceId: Option[String],
  acceptedAt: Option[Instant],
  completedAt: Option[Instant]
)

object LifecycleSupervisorProtocol {
  given Encoder[LifecycleSupervisorRequest] = deriveEncoder
  given Decoder[LifecycleSupervisorResult] = deriveDecoder

  val REQUEST_PATH: String = "/v1/lifecycle-requests"

  def requestBody(request: LifecycleSupervisorRequest): String = request.asJson.noSpaces

  def response(body: String, expectedRequestId: String): Either[String, LifecycleSupervisorResult] =
    decode[LifecycleSupervisorResult](body).left.map(_ => "supervisor-response-invalid").flatMap { result =>
      Either.cond(result.requestId == expectedRequestId, result, "supervisor-response-request-mismatch")
    }

  def requestEndpoint(configuration: LifecycleSupervisorConfiguration): URI =
    configuration.endpoint.resolve(REQUEST_PATH)

  def lookupEndpoint(configuration: LifecycleSupervisorConfiguration, requestId: String): URI =
    configuration.endpoint.resolve(s"$REQUEST_PATH/$requestId")

  def unavailable(
    request: LifecycleSupervisorRequest,
    supervisorId: String,
    diagnosticCode: String,
    now: Instant
  ): LifecycleSupervisorResult =
    LifecycleSupervisorResult(
      request.requestId,
      "rejected",
      Some(diagnosticCode),
      Some(diagnosticCode),
      supervisorId,
      None,
      None,
      Some(now)
    )
}
