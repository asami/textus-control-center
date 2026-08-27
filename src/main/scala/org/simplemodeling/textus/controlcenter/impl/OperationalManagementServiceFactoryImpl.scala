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

final class OperationalManagementServiceFactoryImpl extends TextusControlCenterComponent.OperationalManagementServiceFactory {
  import TextusControlCenterComponent.OperationalManagementService.*

  override def createListOperationalComponentsActionCall(core: ActionCall.Core, action: ListOperationalComponents): ListOperationalComponentsActionCall =
    ListOperationalComponentsActionCallImpl(core, action)
  override def createGetOperationalComponentActionCall(core: ActionCall.Core, action: GetOperationalComponent): GetOperationalComponentActionCall =
    GetOperationalComponentActionCallImpl(core, action)
  override def createRemoveOperationalComponentActionCall(core: ActionCall.Core, action: RemoveOperationalComponent): RemoveOperationalComponentActionCall =
    RemoveOperationalComponentActionCallImpl(core, action)
  override def createRestoreOperationalComponentActionCall(core: ActionCall.Core, action: RestoreOperationalComponent): RestoreOperationalComponentActionCall =
    RestoreOperationalComponentActionCallImpl(core, action)

  private final case class ListOperationalComponentsActionCallImpl(core: ActionCall.Core, override val action: ListOperationalComponents)
      extends ListOperationalComponentsActionCall with OperationalManagementActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        components <- find_operational_components_all
        text = action.record.getString("text").map(_.trim.toLowerCase).filter(_.nonEmpty)
        offset = action.record.getInt("offset").getOrElse(0).max(0)
        limit = action.record.getInt("limit").getOrElse(100).max(0)
        filtered = latest_operational_components(components).filter(component => component.managementState.value != "excluded" && text.forall(value => component.artifactId.value.toLowerCase.contains(value)))
        page = filtered.drop(offset).take(limit)
      } yield OperationResponse(Record.dataAuto("data" -> page.map(safe_projection), "totalCount" -> filtered.size, "offset" -> offset, "limit" -> limit))
  }

  private final case class GetOperationalComponentActionCallImpl(core: ActionCall.Core, override val action: GetOperationalComponent)
      extends GetOperationalComponentActionCall with OperationalManagementActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(action.record, "artifactId"))
        components <- find_operational_components(artifactid)
        component <- exec_from(latest_operational_component(components).toRight(RegistryError.Missing(artifactid)).fold(registry_error, Consequence.success))
      } yield OperationResponse(safe_projection(component))
  }

  private final case class RemoveOperationalComponentActionCallImpl(core: ActionCall.Core, override val action: RemoveOperationalComponent)
      extends RemoveOperationalComponentActionCall with OperationalManagementActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(action.record, "artifactId"))
        components <- find_operational_components(artifactid)
        component <- exec_from(latest_operational_component(components).toRight(RegistryError.Missing(artifactid)).fold(registry_error, Consequence.success))
        now = core.executionContext.clock.instant()
        patch <- exec_from(operational_component_update("excluded", now))
        _ <- entity_update(component.id, patch)
        stored = component.copy(managementState = OperationalManagementStateValue("excluded"), lastObservedAt = now)
      } yield OperationResponse(safe_projection(stored))
  }

  private final case class RestoreOperationalComponentActionCallImpl(core: ActionCall.Core, override val action: RestoreOperationalComponent)
      extends RestoreOperationalComponentActionCall with OperationalManagementActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(action.record, "artifactId"))
        components <- find_operational_components(artifactid)
        component <- exec_from(latest_operational_component(components).toRight(RegistryError.Missing(artifactid)).fold(registry_error, Consequence.success))
        sources <- find_managed_sources_all
        registered <- find_registered_subsystems_all
        developmentavailable = latest_managed_sources(sources).exists(source => source.artifactId.value == artifactid && source.sourceKind.value == "DEV" && source.refreshState.value == "available")
        accepteduse = registered.exists(_.artifactId.exists(_.value == artifactid))
        state <- exec_from(OperationalComponentManagement.reconcile(Some(OperationalManagementState.Excluded), developmentavailable, accepteduse, exclusionrecordexists = false).toRight("No development source or accepted launcher use evidence exists.").fold(Consequence.operationInvalid, Consequence.success))
        now = core.executionContext.clock.instant()
        patch <- exec_from(operational_component_update(state.mark, now))
        _ <- entity_update(component.id, patch)
        stored = component.copy(managementState = OperationalManagementStateValue(state.mark), lastObservedAt = now)
      } yield OperationResponse(safe_projection(stored))
  }

  private trait OperationalManagementActionSupport { self: ActionCall =>
    protected final def administrative_principal: Consequence[Unit] = {
      val subject = SecuritySubject.current(using executionContext)
      val privileges = Vector(SecurityContext.Privilege.ApplicationContentManager, SecurityContext.Privilege.Operator, SecurityContext.Privilege.System, SecurityContext.Privilege.Internal)
      if (subject.isAuthenticated && privileges.exists(privilege => subject.hasPrivilege(privilege.name) || subject.hasCapability(privilege.name) || subject.hasRole(privilege.name))) Consequence.unit
      else Consequence.securityPermissionDenied("Operational component management requires administrative authorization.")
    }
    protected final def required_string(record: Record, name: String): Consequence[String] =
      record.getString(name).map(_.trim).filter(_.nonEmpty).toRight(s"$name is required").fold(Consequence.operationInvalid, Consequence.success)
    protected final def registry_error(error: RegistryError): Consequence[Nothing] = error match {
      case RegistryError.Missing(_) => Consequence.resourceNotFound(error.message)
      case _ => Consequence.operationInvalid(error.message)
    }
    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId.value == artifactid)
    protected final def find_operational_components_all: ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data
    protected final def find_managed_sources_all: ExecUowM[Vector[ManagedCarSourceEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "ManagedCarSource")); query = EntityQuery[ManagedCarSourceEntity](ManagedCarSourceQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[ManagedCarSourceEntity](query) } yield result.data
    protected final def find_registered_subsystems_all: ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem")); query = EntityQuery[RegisteredSubsystemEntity](RegisteredSubsystemQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[RegisteredSubsystemEntity](query) } yield result.data
    protected final def latest_operational_component(sources: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] = sources.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption
    protected final def latest_operational_components(sources: Vector[OperationalComponentEntity]): Vector[OperationalComponentEntity] = sources.groupBy(_.artifactId.value).valuesIterator.flatMap(latest_operational_component).toVector.sortBy(source => (if (source.artifactId.value == "textus-control-center") 0 else 1, source.artifactId.value))
    protected final def latest_managed_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] = sources.groupBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value)).valuesIterator.flatMap(source => source.sortBy(value => (value.snapshotAt, value.id.print)).lastOption).toVector
    protected final def operational_component_update(managementstate: String, now: Instant): Consequence[OperationalComponentUpdate] =
      new OperationalComponentUpdate.Builder().withManagementState(OperationalManagementStateValue(managementstate)).withLastObservedAt(now).buildC()
    protected final def safe_projection(component: OperationalComponentEntity): Record = Record.dataAuto("artifactId" -> component.artifactId.value, "managementState" -> component.managementState.value, "firstManagedAt" -> component.firstManagedAt, "lastObservedAt" -> component.lastObservedAt)
  }
}
