/*
 * @since   Jul. 22, 2026
 * @version Jul. 22, 2026
 * @author  ASAMI, Tomoharu
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.time.Instant

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode

final case class LauncherEvidenceEntry(
  launcherKind: String,
  instanceId: String,
  target: String,
  artifactId: Option[String],
  executionMode: String,
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: String,
  startedAt: Instant,
  lastSeenAt: Instant,
  stoppedAt: Option[Instant]
)

final case class LauncherEvidenceListProjection(
  schema: String,
  entries: Vector[LauncherEvidenceEntry]
)

final case class LauncherEvidenceDetailEntry(
  launcherKind: String,
  instanceId: String,
  target: String,
  artifactId: Option[String],
  executionMode: String,
  developmentDirectory: Option[String],
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: String,
  startedAt: Instant,
  lastSeenAt: Instant,
  stoppedAt: Option[Instant]
)

final case class LauncherEvidenceDetailProjection(
  schema: String,
  entry: LauncherEvidenceDetailEntry
)

object LauncherEvidenceProtocol {
  val Schema = "cncf.launcher.evidence-projection.v1"
  val InvalidProjection = "launcher-evidence-projection-invalid"

  given Decoder[LauncherEvidenceEntry] = deriveDecoder
  given Decoder[LauncherEvidenceListProjection] = deriveDecoder
  given Decoder[LauncherEvidenceDetailEntry] = deriveDecoder
  given Decoder[LauncherEvidenceDetailProjection] = deriveDecoder

  def list(value: String): Either[String, LauncherEvidenceListProjection] =
    decode[LauncherEvidenceListProjection](value).left.map(_ => InvalidProjection).flatMap { projection =>
      if (projection.schema == Schema && projection.entries.map(_.instanceId).distinct.size == projection.entries.size)
        Right(projection.copy(entries = projection.entries.sortBy(entry => (entry.target, entry.instanceId))))
      else
        Left(InvalidProjection)
    }

  def detail(value: String, instanceid: String): Either[String, LauncherEvidenceDetailProjection] =
    decode[LauncherEvidenceDetailProjection](value).left.map(_ => InvalidProjection).flatMap { projection =>
      if (projection.schema == Schema && projection.entry.instanceId == instanceid)
        Right(projection)
      else
        Left(InvalidProjection)
    }
}
