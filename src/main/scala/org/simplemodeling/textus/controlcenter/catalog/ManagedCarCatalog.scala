/*
 *  version Jul. 21, 2026
 * @version Aug. 10, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.time.Instant

enum ManagedCarSourceKind(val mark: String) {
  case Development extends ManagedCarSourceKind("DEV")
  case LocalRepository extends ManagedCarSourceKind("LOCAL")
  case PublicRepository extends ManagedCarSourceKind("PUBLIC")
}

enum ManagedCarRefreshState {
  case Available, Unavailable, Invalid
}

enum RuntimeInstanceStatus {
  case Starting, Running, Stale, Stopped
}

enum RuntimeLinkKind {
  case Direct, Legacy, Unlinked, Ambiguous
}

enum ManagedCarRuntimeState {
  case Starting, Running, NotRunning
}

final case class ManagedCarSource(
  artifactId: String,
  sourceKind: ManagedCarSourceKind,
  sourceId: String,
  componentName: Option[String],
  availableVersions: Vector[String],
  refreshState: ManagedCarRefreshState,
  snapshotAt: Instant,
  diagnostic: Option[String],
  privateLocator: Option[String]
) {
  def launchProfileId: String = s"${sourceKind.mark}:$sourceId"
}

final case class ManagedCarSourceProjection(
  artifactId: String,
  sourceKind: ManagedCarSourceKind,
  sourceId: String,
  componentName: Option[String],
  availableVersions: Vector[String],
  refreshState: ManagedCarRefreshState,
  snapshotAt: Instant,
  diagnostic: Option[String]
)

final case class ManagedCar(
  artifactId: String,
  componentName: Option[String],
  legacyAliases: Set[String],
  sources: Vector[ManagedCarSource],
  diagnostics: Vector[String]
)

final case class RuntimeInstance(
  instanceId: String,
  artifactId: Option[String],
  target: String,
  componentName: Option[String],
  status: RuntimeInstanceStatus
)

final case class RuntimeLink(
  instanceId: String,
  artifactId: Option[String],
  kind: RuntimeLinkKind,
  diagnostic: Option[String]
)

final case class ManagedCarRuntimeSummary(
  state: ManagedCarRuntimeState,
  activeInstanceIds: Vector[String],
  staleInstanceIds: Vector[String]
)

object ManagedCarCatalog {
  def mergeSources(
    sources: Vector[ManagedCarSource],
    legacyAliases: Map[String, Set[String]] = Map.empty
  ): Vector[ManagedCar] =
    sources
      .filter(source => source.artifactId.trim.nonEmpty)
      .groupBy(_.artifactId.trim)
      .toVector
      .sortBy(_._1)
      .map { case (artifactid, grouped) =>
        val ordered = grouped.sortBy(source => (source.sourceKind.mark, source.sourceId))
        val components = ordered.flatMap(_.componentName.map(_.trim).filter(_.nonEmpty)).distinct
        val diagnostics = if (components.size <= 1) Vector.empty else Vector("conflicting-component-identity")
        ManagedCar(
          artifactid,
          if (components.size == 1) components.headOption else None,
          legacyAliases.getOrElse(artifactid, Set.empty),
          ordered,
          diagnostics
        )
      }

  def sourceProjection(source: ManagedCarSource): ManagedCarSourceProjection =
    ManagedCarSourceProjection(
      source.artifactId,
      source.sourceKind,
      source.sourceId,
      source.componentName,
      source.availableVersions,
      source.refreshState,
      source.snapshotAt,
      source.diagnostic
    )

  def linkRuntimeInstances(
    cars: Vector[ManagedCar],
    instances: Vector[RuntimeInstance]
  ): Vector[RuntimeLink] =
    instances.sortBy(_.instanceId).map { instance =>
      instance.artifactId.map(_.trim).filter(_.nonEmpty) match {
        case Some(artifactid) =>
          if (cars.exists(_.artifactId == artifactid)) {
            RuntimeLink(instance.instanceId, Some(artifactid), RuntimeLinkKind.Direct, None)
          } else {
            RuntimeLink(instance.instanceId, None, RuntimeLinkKind.Unlinked, Some("unmanaged-artifact-id"))
          }
        case None =>
          _legacy_matches(instance, cars) match {
            case Vector(car) => RuntimeLink(instance.instanceId, Some(car.artifactId), RuntimeLinkKind.Legacy, None)
            case Vector() => RuntimeLink(instance.instanceId, None, RuntimeLinkKind.Unlinked, None)
            case _ => RuntimeLink(instance.instanceId, None, RuntimeLinkKind.Ambiguous, Some("ambiguous-legacy-runtime-link"))
          }
      }
    }

  def runtimeSummary(
    artifactId: String,
    instances: Vector[RuntimeInstance],
    links: Vector[RuntimeLink]
  ): ManagedCarRuntimeSummary = {
    val linkedids = links.collect {
      case RuntimeLink(instanceid, Some(linkedartifactid), kind, _) if linkedartifactid == artifactId &&
        (kind == RuntimeLinkKind.Direct || kind == RuntimeLinkKind.Legacy) => instanceid
    }.toSet
    val linked = instances.filter(instance => linkedids.contains(instance.instanceId))
    val active = linked.collect {
      case instance @ RuntimeInstance(_, _, _, _, RuntimeInstanceStatus.Starting | RuntimeInstanceStatus.Running) => instance
    }.sortBy(_.instanceId)
    val stale = linked.collect {
      case instance @ RuntimeInstance(_, _, _, _, RuntimeInstanceStatus.Stale) => instance
    }.sortBy(_.instanceId)
    val state = if (active.exists(_.status == RuntimeInstanceStatus.Running)) {
      ManagedCarRuntimeState.Running
    } else if (active.nonEmpty) {
      ManagedCarRuntimeState.Starting
    } else {
      ManagedCarRuntimeState.NotRunning
    }
    ManagedCarRuntimeSummary(state, active.map(_.instanceId), stale.map(_.instanceId))
  }

  private def _legacy_matches(
    instance: RuntimeInstance,
    cars: Vector[ManagedCar]
  ): Vector[ManagedCar] = {
    val aliases = Set(instance.target.trim).filter(_.nonEmpty) ++ instance.componentName.map(_.trim).filter(_.nonEmpty)
    cars.filter(car => car.legacyAliases.intersect(aliases).nonEmpty).sortBy(_.artifactId)
  }
}
