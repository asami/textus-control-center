/*
 * @version Jul. 27, 2026
 */
package org.simplemodeling.textus.controlcenter.launcher

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import org.goldenport.Consequence

object LauncherEvidenceSnapshotCodec {
  val schemaVersion: String = "textus-control-center.launcher-evidence-snapshot.v1"

  final case class Snapshot(
    schema: String,
    instanceId: String,
    launcherKind: String,
    target: String,
    artifactId: Option[String],
    executionMode: String,
    subsystemName: Option[String],
    subsystemVersion: Option[String],
    runtimeVersion: String,
    startedAt: String,
    lastSeenAt: String,
    stoppedAt: Option[String],
    observedAt: String,
    evidenceDecision: String
  )

  object Snapshot {
    given Encoder[Snapshot] = deriveEncoder
    given Decoder[Snapshot] = deriveDecoder
  }

  def encode(entry: LauncherEvidenceEntry, evidenceDecision: String, observedAt: Instant): String =
    Snapshot(schemaVersion, entry.instanceId, entry.launcherKind, entry.target, entry.artifactId, entry.executionMode, entry.subsystemName, entry.subsystemVersion, entry.runtimeVersion, entry.startedAt.toString, entry.lastSeenAt.toString, entry.stoppedAt.map(_.toString), observedAt.toString, evidenceDecision).asJson.noSpaces

  def decode(payload: String): Consequence[Snapshot] =
    io.circe.parser.decode[Snapshot](payload).fold(
      error => Consequence.operationInvalid(s"Launcher evidence snapshot is invalid: ${error.getMessage}"),
      snapshot => if (snapshot.schema == schemaVersion) Consequence.success(snapshot) else Consequence.operationInvalid(s"Launcher evidence snapshot schema is unsupported: ${snapshot.schema}")
    )
}
