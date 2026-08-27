/*
 * @since   Aug. 28, 2026
 * @version Aug. 28, 2026
 */
package org.simplemodeling.textus.controlcenter.impl

import java.net.{URI, URL}
import java.time.{Duration, Instant}
import scala.util.control.NonFatal

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.datatype.I18nLabel
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityQuery, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.context.SecurityContext
import org.goldenport.cncf.event.{CmlEventCategory, CmlEventDefinition, CmlSubscriptionDefinition, DispatchRoute, EventOriginBoundary, EventReceptionCondition, EventReceptionExecutionPolicy, EventReceptionRule, ReceptionDomainEvent}
import org.goldenport.cncf.security.AuthenticationProvider
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.spi.supervisor.SupervisorSocket
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.goldenport.schema.XString
import org.simplemodeling.textus.controlcenter.TextusControlCenterComponent
import org.simplemodeling.model.directive.Update
import org.simplemodeling.textus.controlcenter.launcher.LauncherEvidenceSnapshotCodec
import org.simplemodeling.textus.controlcenter.entity.{RegisteredSubsystem as RegisteredSubsystemEntity}
import org.simplemodeling.textus.controlcenter.entity.{ManagedCar as ManagedCarEntity, ManagedCarSource as ManagedCarSourceEntity}
import org.simplemodeling.textus.controlcenter.entity.{OperationalComponent as OperationalComponentEntity}
import org.simplemodeling.textus.controlcenter.entity.{LifecycleRequest as LifecycleRequestEntity}
import org.simplemodeling.textus.controlcenter.entity.{LauncherEvidenceSnapshot as LauncherEvidenceSnapshotEntity}
import org.simplemodeling.textus.controlcenter.entity.create.{RegisteredSubsystem as RegisteredSubsystemCreate}
import org.simplemodeling.textus.controlcenter.entity.create.RegisteredSubsystem.given
import org.simplemodeling.textus.controlcenter.entity.query.{RegisteredSubsystem as RegisteredSubsystemQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{ManagedCar as ManagedCarQuery, ManagedCarSource as ManagedCarSourceQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{OperationalComponent as OperationalComponentQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{LifecycleRequest as LifecycleRequestQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{LauncherEvidenceSnapshot as LauncherEvidenceSnapshotQuery}
import org.simplemodeling.textus.controlcenter.entity.create.{ManagedCar as ManagedCarCreate, ManagedCarSource as ManagedCarSourceCreate}
import org.simplemodeling.textus.controlcenter.entity.create.{OperationalComponent as OperationalComponentCreate}
import org.simplemodeling.textus.controlcenter.entity.create.{LifecycleRequest as LifecycleRequestCreate}
import org.simplemodeling.textus.controlcenter.entity.create.{LauncherEvidenceSnapshot as LauncherEvidenceSnapshotCreate}
import org.simplemodeling.textus.controlcenter.entity.update.{LauncherEvidenceSnapshot as LauncherEvidenceSnapshotUpdate}
import org.simplemodeling.textus.controlcenter.entity.update.{LifecycleRequest as LifecycleRequestUpdate, ManagedCar as ManagedCarUpdate, OperationalComponent as OperationalComponentUpdate}
import org.simplemodeling.textus.controlcenter.datatype.{DevelopmentDirectory as DevelopmentDirectoryValue, ExecutionMode as ExecutionModeValue, LauncherEvidenceDecision as LauncherEvidenceDecisionValue, LauncherEvidenceSnapshotPayload as LauncherEvidenceSnapshotPayloadValue, LauncherKind as LauncherKindValue, LauncherState as LauncherStateValue, LifecycleAction as LifecycleActionValue, LifecycleDiagnostic as LifecycleDiagnosticValue, LifecycleDiagnosticCode as LifecycleDiagnosticCodeValue, LifecycleIdempotencyKey as LifecycleIdempotencyKeyValue, LifecycleLaunchProfileId as LifecycleLaunchProfileIdValue, LifecycleRequestId as LifecycleRequestIdValue, LifecycleRequestState as LifecycleRequestStateValue, LifecycleSupervisorId as LifecycleSupervisorIdValue, ManagedCarArtifactId as ManagedCarArtifactIdValue, ManagedCarComponentName as ManagedCarComponentNameValue, ManagedCarDiagnostic as ManagedCarDiagnosticValue, ManagedCarPrivateLocator as ManagedCarPrivateLocatorValue, ManagedCarRefreshState as ManagedCarRefreshStateValue, ManagedCarSourceId as ManagedCarSourceIdValue, ManagedCarSourceKind as ManagedCarSourceKindValue, ManagedCarVersion as ManagedCarVersionValue, OperationalManagementState as OperationalManagementStateValue, OperatorSubjectId as OperatorSubjectIdValue, RegistrationPrincipalId as RegistrationPrincipalIdValue, RuntimeVersion as RuntimeVersionValue, SubsystemInstanceId as SubsystemInstanceIdValue, SubsystemName as SubsystemNameValue, SubsystemTarget as SubsystemTargetValue, SubsystemVersion as SubsystemVersionValue}
import org.simplemodeling.textus.controlcenter.entity.create.ManagedCar.given
import org.simplemodeling.textus.controlcenter.entity.create.ManagedCarSource.given
import org.simplemodeling.textus.controlcenter.entity.create.OperationalComponent.given
import org.simplemodeling.textus.controlcenter.entity.create.LifecycleRequest.given
import org.simplemodeling.textus.controlcenter.catalog.{DevelopmentRoot, LocalRepositoryCatalog, ManagedCar as CatalogManagedCar, ManagedCarCatalog, ManagedCarRuntimeState, ManagedCarRuntimeSummary, ManagedCarSource as CatalogManagedCarSource, OperationalComponentManagement, OperationalManagementState, PublicRepositoryCatalog, RuntimeInstance, RuntimeInstanceStatus, StandaloneCatalogConfiguration, StandaloneDevelopmentCatalogProvider, StandaloneLocalRepositoryCatalogProvider, StandalonePublicRepositoryCatalogProvider}
import org.simplemodeling.textus.controlcenter.registry.{ApplicationUrlPolicy, RegisteredSubsystem as RegistrySubsystem, RegistryError, RegistrationInput, SubsystemRegistry}
import org.simplemodeling.textus.controlcenter.supervisor.{EmbeddedTextusSupervisor, LifecycleSupervisorProtocol, LifecycleSupervisorRequest, LifecycleSupervisorResult, TextusSupervisor}
import org.simplemodeling.textus.controlcenter.launcher.{LauncherEvidenceClient, LauncherEvidenceClientConfiguration, LauncherEvidenceEntry}

final class LauncherEvidenceServiceFactoryImpl extends TextusControlCenterComponent.LauncherEvidenceServiceFactory {
  import TextusControlCenterComponent.LauncherEvidenceService.*

  override def createRefreshLauncherEvidenceActionCall(core: ActionCall.Core, action: RefreshLauncherEvidence): RefreshLauncherEvidenceActionCall =
    RefreshLauncherEvidenceActionCallImpl(core, action)
  override def createListLauncherEvidenceActionCall(core: ActionCall.Core, action: ListLauncherEvidence): ListLauncherEvidenceActionCall =
    ListLauncherEvidenceActionCallImpl(core, action)
  override def createGetLauncherEvidenceActionCall(core: ActionCall.Core, action: GetLauncherEvidence): GetLauncherEvidenceActionCall =
    GetLauncherEvidenceActionCallImpl(core, action)

  private final case class RefreshLauncherEvidenceActionCallImpl(core: ActionCall.Core, override val action: RefreshLauncherEvidence)
      extends RefreshLauncherEvidenceActionCall with LauncherEvidenceActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        evidenceclient <- exec_from(launcher_evidence_client.fold(Consequence.operationInvalid, Consequence.success))
        projection <- exec_from(evidenceclient.list().fold(Consequence.operationInvalid, Consequence.success))
        existing <- find_all
        registered <- find_registered_subsystems_all
        now = core.executionContext.clock.instant()
        records <- projection.entries.traverse(entry => retain(entry, existing, registered.filterNot(_.launcherState.value == "stopped").map(_.instanceId.value).toSet, now))
        snapshots <- exec_from(records.traverse(snapshot_payload))
      } yield OperationResponse(Record.dataAuto("data" -> snapshots.map(safe_projection), "totalCount" -> snapshots.size, "observedAt" -> now))
  }

  private final case class ListLauncherEvidenceActionCallImpl(core: ActionCall.Core, override val action: ListLauncherEvidence)
      extends ListLauncherEvidenceActionCall with LauncherEvidenceActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        values <- find_all
        text = action.record.getString("text").map(_.trim.toLowerCase).filter(_.nonEmpty)
        offset = action.record.getInt("offset").getOrElse(0).max(0)
        limit = action.record.getInt("limit").getOrElse(100).max(0)
        snapshots <- exec_from(latest(values).traverse(snapshot_payload))
        records = snapshots.filter { record =>
          text.forall(value => Vector(record.launcherKind, record.target, record.instanceId).exists(_.toLowerCase.contains(value)))
        }
        page = records.drop(offset).take(limit)
      } yield OperationResponse(Record.dataAuto("data" -> page.map(safe_projection), "totalCount" -> records.size, "offset" -> offset, "limit" -> limit))
  }

  private final case class GetLauncherEvidenceActionCallImpl(core: ActionCall.Core, override val action: GetLauncherEvidence)
      extends GetLauncherEvidenceActionCall with LauncherEvidenceActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        instanceid <- exec_from(required_string(action.record, "instanceId"))
        values <- find_all
        record <- exec_from(latest(values).find(_.instanceId.value == instanceid).toRight(instanceid).fold(Consequence.resourceNotFound, Consequence.success))
        snapshot <- exec_from(snapshot_payload(record))
        detail = launcher_evidence_client.toOption.flatMap(_.detail(instanceid).toOption)
      } yield OperationResponse(Record.dataAuto(
        "instanceId" -> snapshot.instanceId,
        "launcherKind" -> snapshot.launcherKind,
        "target" -> snapshot.target,
        "artifactId" -> snapshot.artifactId,
        "executionMode" -> snapshot.executionMode,
        "subsystemName" -> snapshot.subsystemName,
        "subsystemVersion" -> snapshot.subsystemVersion,
        "runtimeVersion" -> snapshot.runtimeVersion,
        "startedAt" -> snapshot.startedAt,
        "lastSeenAt" -> snapshot.lastSeenAt,
        "stoppedAt" -> snapshot.stoppedAt,
        "evidenceDecision" -> snapshot.evidenceDecision,
        "observedAt" -> record.observedAt,
        "developmentDirectory" -> detail.flatMap(_.entry.developmentDirectory),
        "detailDiagnostic" -> (if (detail.isDefined) None else Some("launcher-evidence-detail-unavailable"))
      ))
  }

  private trait LauncherEvidenceActionSupport { self: ActionCall =>
    protected final def administrative_principal: Consequence[Unit] = {
      val subject = SecuritySubject.current(using executionContext)
      val privileges = Vector(SecurityContext.Privilege.ApplicationContentManager, SecurityContext.Privilege.Operator, SecurityContext.Privilege.System, SecurityContext.Privilege.Internal)
      if (subject.isAuthenticated && privileges.exists(privilege => subject.hasPrivilege(privilege.name) || subject.hasCapability(privilege.name) || subject.hasRole(privilege.name))) Consequence.unit
      else Consequence.securityPermissionDenied("Launcher evidence requires administrative authorization.")
    }
    protected final def required_string(record: Record, name: String): Consequence[String] =
      record.getString(name).map(_.trim).filter(_.nonEmpty).toRight(s"$name is required").fold(Consequence.operationInvalid, Consequence.success)
    protected final def launcher_evidence_client: Either[String, LauncherEvidenceClient] = {
      val props = Vector(LauncherEvidenceClientConfiguration.Command, LauncherEvidenceClientConfiguration.Timeout).flatMap(key => config_string(key).map(key -> _)).toMap
      LauncherEvidenceClientConfiguration.fromProperties(props).map(LauncherEvidenceClient(_))
    }
    protected final def find_all: ExecUowM[Vector[LauncherEvidenceSnapshotEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "LauncherEvidenceSnapshot")); query = EntityQuery[LauncherEvidenceSnapshotEntity](LauncherEvidenceSnapshotQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[LauncherEvidenceSnapshotEntity](query) } yield result.data
    protected final def find_registered_subsystems_all: ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem")); query = EntityQuery[RegisteredSubsystemEntity](RegisteredSubsystemQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[RegisteredSubsystemEntity](query) } yield result.data
    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId.value == artifactid)
    protected final def latest(values: Vector[LauncherEvidenceSnapshotEntity]): Vector[LauncherEvidenceSnapshotEntity] =
      values.groupBy(_.instanceId.value).valuesIterator.flatMap(_.sortBy(value => (value.observedAt, value.id.print)).lastOption).toVector.sortBy(_.instanceId.value)
    protected final def latest_operational_component(values: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      values.sortBy(value => (value.lastObservedAt, value.id.print)).lastOption
    protected final def retain(entry: LauncherEvidenceEntry, existing: Vector[LauncherEvidenceSnapshotEntity], registeredids: Set[String], now: Instant): ExecUowM[LauncherEvidenceSnapshotEntity] = {
      val decision = if (entry.stoppedAt.isDefined) "historical-stopped" else if (registeredids.contains(entry.instanceId)) "current-registered" else "current-evidence-only"
      val snapshot = snapshot_payload(entry, decision, now)
      for {
        record <- latest(existing).find(_.instanceId.value == entry.instanceId) match {
          case Some(current) =>
            for {
              patch <- exec_from(new LauncherEvidenceSnapshotUpdate.Builder().withSnapshotPayload(LauncherEvidenceSnapshotPayloadValue(snapshot)).withObservedAt(now).buildC())
              _ <- entity_update(current.id, patch)
            } yield current.copy(snapshotPayload = LauncherEvidenceSnapshotPayloadValue(snapshot), observedAt = now)
          case None =>
            entity_create(LauncherEvidenceSnapshotCreate(None, SubsystemInstanceIdValue(entry.instanceId), LauncherEvidenceSnapshotPayloadValue(snapshot), now)).map { stored =>
              LauncherEvidenceSnapshotEntity(stored.id, SubsystemInstanceIdValue(entry.instanceId), LauncherEvidenceSnapshotPayloadValue(snapshot), now)
            }
        }
        _ <- entry.artifactId match {
          case Some(artifactid) if entry.stoppedAt.isEmpty => _retain_adoption(artifactid, now)
          case _ => exec_pure(())
        }
      } yield record
    }
    private def _retain_adoption(artifactid: String, now: Instant): ExecUowM[Unit] =
      for {
        components <- find_operational_components(artifactid)
        _ <- latest_operational_component(components) match {
          case Some(_) => exec_pure(())
          case None => entity_create(OperationalComponentCreate(None, ManagedCarArtifactIdValue(artifactid), OperationalManagementStateValue("adopted"), now, now)).map(_ => ())
        }
      } yield ()
    protected final def snapshot_payload(entry: LauncherEvidenceEntry, decision: String, observedat: Instant): String =
      LauncherEvidenceSnapshotCodec.encode(entry, decision, observedat)
    protected final def snapshot_payload(record: LauncherEvidenceSnapshotEntity): Consequence[LauncherEvidenceSnapshotCodec.Snapshot] =
      LauncherEvidenceSnapshotCodec.decode(record.snapshotPayload.value)
    protected final def safe_projection(record: LauncherEvidenceSnapshotCodec.Snapshot): Record = Record.dataAuto(
      "instanceId" -> record.instanceId, "launcherKind" -> record.launcherKind, "target" -> record.target, "artifactId" -> record.artifactId,
      "executionMode" -> record.executionMode, "subsystemName" -> record.subsystemName, "subsystemVersion" -> record.subsystemVersion,
      "runtimeVersion" -> record.runtimeVersion, "startedAt" -> record.startedAt, "lastSeenAt" -> record.lastSeenAt,
      "stoppedAt" -> record.stoppedAt, "evidenceDecision" -> record.evidenceDecision, "observedAt" -> record.observedAt
    )
  }
}
