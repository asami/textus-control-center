/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
package org.simplemodeling.textus.controlcenter.summary

import java.time.{Duration, Instant}
import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.component.Component
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.registry.{RegisteredSubsystem, RegistryError, SubsystemRegistry}

object OperationalSummary {
  opaque type Limit = Int

  final case class RuntimeMetadata(bootedAt: Instant, runtimeVersion: String)
  final case class StoredInstance(recordId: String, value: RegisteredSubsystem)

  def createLimitC(requested: Option[Int]): Consequence[Limit] = {
    val limit = requested.getOrElse(50)
    if (limit >= 1 && limit <= 100) Consequence.success(limit)
    else Consequence.operationInvalid("limit must be between 1 and 100")
  }

  def project(
    componentId: String,
    instanceId: String,
    version: Option[String],
    runtimeMetadata: Option[RuntimeMetadata],
    health: Component.HealthSnapshot,
    sources: Vector[StoredInstance],
    now: Instant,
    limit: Limit
  ): Consequence[Record] =
    runtimes(sources, now, limit).map { runtimes =>
      Record.data(
        "protocolVersion" -> 1,
        "observedAt" -> now.toString,
        "componentId" -> componentId,
        "instanceId" -> instanceId,
        "controlCenter" -> Record.data(
          "status" -> "available",
          "version" -> metadata(version.filterNot(_.equalsIgnoreCase("unknown"))),
          "uptime" -> uptime(runtimeMetadata.map(_.bootedAt), now),
          "runtimeVersion" -> metadata(runtimeMetadata.map(_.runtimeVersion))
        ),
        "overallHealth" -> Record.data(
          "status" -> health.status,
          "scope" -> "control-center",
          "checks" -> health.checks.map(check => Record.data("name" -> check.name, "status" -> check.status))
        ),
        "runtimes" -> runtimes,
        "dashboardUrl" -> "/web/textus-control-center",
        "refreshAfterSeconds" -> 15
      )
    }

  def metadata(value: Option[String]): Record = {
    val available = value.map(_.trim).filter(_.nonEmpty)
    Record.data("state" -> (if (available.isDefined) "available" else "unavailable")) ++
      Record.dataOption("value" -> available)
  }

  def uptime(bootedAt: Option[Instant], now: Instant): Record =
    bootedAt match {
      case Some(bootedat) => Record.data(
        "state" -> "available",
        "seconds" -> Duration.between(bootedat, now).getSeconds.max(0L)
      )
      case None => Record.data("state" -> "unavailable")
    }

  def runtimes(sources: Vector[StoredInstance], now: Instant, limit: Limit): Consequence[Record] = {
    val latest = sources.groupBy(_.value.instanceId).valuesIterator.map { group =>
      group.maxBy(source => (source.value.lastSeenAt, source.recordId)).value
    }.toVector
    val ordered = SubsystemRegistry.ordered(latest)
    ordered.traverse(source => SubsystemRegistry.projection(source, now, _stale_threshold))
      .fold(_registry_error, projections => {
        val data = projections.take(limit).map { projection =>
          Record.data(
            "instanceId" -> projection.instanceId,
            "target" -> projection.target,
            "launcherState" -> projection.launcherState,
            "status" -> projection.status
          ) ++ Record.dataOption(
            "artifactId" -> projection.artifactId,
            "subsystemName" -> projection.subsystemName,
            "subsystemVersion" -> projection.subsystemVersion,
            "runtimeVersion" -> projection.runtimeVersion
          )
        }
        Consequence.success(Record.data(
          "total" -> projections.size,
          "returned" -> data.size,
          "truncated" -> (data.size < projections.size),
          "starting" -> projections.count(_.status == SubsystemRegistry.starting),
          "running" -> projections.count(_.status == SubsystemRegistry.running),
          "stopped" -> projections.count(_.status == SubsystemRegistry.stopped),
          "stale" -> projections.count(_.status == SubsystemRegistry.stale),
          "health" -> "unavailable",
          "data" -> data
        ))
      })
  }

  private val _stale_threshold = Duration.ofSeconds(90)

  private def _registry_error(error: RegistryError): Consequence[Nothing] =
    error match {
      case RegistryError.Invalid(message) => Consequence.operationInvalid(message)
      case RegistryError.Missing(_) => Consequence.resourceNotFound(error.message)
      case RegistryError.Conflict(_, _) => Consequence.stateConflict(error.message)
      case RegistryError.Unauthorized(_) => Consequence.securityPermissionDenied(error.message)
    }
}
