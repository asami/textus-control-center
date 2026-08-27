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

final class LifecycleControlServiceFactoryImpl extends TextusControlCenterComponent.LifecycleControlServiceFactory {
  import TextusControlCenterComponent.LifecycleControlService.*

  override def createStartOperationalComponentActionCall(core: ActionCall.Core, action: StartOperationalComponent): StartOperationalComponentActionCall =
    StartOperationalComponentActionCallImpl(core, action)
  override def createStopOperationalComponentActionCall(core: ActionCall.Core, action: StopOperationalComponent): StopOperationalComponentActionCall =
    StopOperationalComponentActionCallImpl(core, action)
  override def createRestartOperationalComponentActionCall(core: ActionCall.Core, action: RestartOperationalComponent): RestartOperationalComponentActionCall =
    RestartOperationalComponentActionCallImpl(core, action)
  override def createDispatchLifecycleRequestActionCall(core: ActionCall.Core, action: DispatchLifecycleRequest): DispatchLifecycleRequestActionCall =
    DispatchLifecycleRequestActionCallImpl(core, action)
  override def createListLifecycleRequestsActionCall(core: ActionCall.Core, action: ListLifecycleRequests): ListLifecycleRequestsActionCall =
    ListLifecycleRequestsActionCallImpl(core, action)
  override def createGetLifecycleRequestActionCall(core: ActionCall.Core, action: GetLifecycleRequest): GetLifecycleRequestActionCall =
    GetLifecycleRequestActionCallImpl(core, action)

  private final case class StartOperationalComponentActionCallImpl(core: ActionCall.Core, override val action: StartOperationalComponent)
      extends StartOperationalComponentActionCall with LifecycleControlActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] = lifecycle_request("start", action.record)
  }

  private final case class StopOperationalComponentActionCallImpl(core: ActionCall.Core, override val action: StopOperationalComponent)
      extends StopOperationalComponentActionCall with LifecycleControlActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] = lifecycle_request("stop", action.record)
  }

  private final case class RestartOperationalComponentActionCallImpl(core: ActionCall.Core, override val action: RestartOperationalComponent)
      extends RestartOperationalComponentActionCall with LifecycleControlActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] = lifecycle_request("restart", action.record)
  }

  private final case class DispatchLifecycleRequestActionCallImpl(core: ActionCall.Core, override val action: DispatchLifecycleRequest)
      extends DispatchLifecycleRequestActionCall with LifecycleControlActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        requestid <- exec_from(required_string(action.record, "requestId"))
        requests <- find_lifecycle_requests_all
        request <- exec_from(requests.find(_.requestId.value == requestid).toRight(requestid).fold(Consequence.resourceNotFound, Consequence.success))
        result <- dispatch_lifecycle_request(request)
      } yield OperationResponse(safe_projection(result))
  }

  private final case class ListLifecycleRequestsActionCallImpl(core: ActionCall.Core, override val action: ListLifecycleRequests)
      extends ListLifecycleRequestsActionCall with LifecycleControlActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(action.record, "artifactId"))
        requests <- find_lifecycle_requests(artifactid)
        offset = action.record.getInt("offset").getOrElse(0).max(0)
        limit = action.record.getInt("limit").getOrElse(100).max(0)
        page = requests.sortBy(value => (value.requestedAt, value.id.print)).reverse.drop(offset).take(limit)
      } yield OperationResponse(Record.dataAuto(
        "data" -> page.map(safe_projection),
        "totalCount" -> requests.size,
        "offset" -> offset,
        "limit" -> limit
      ))
  }

  private final case class GetLifecycleRequestActionCallImpl(core: ActionCall.Core, override val action: GetLifecycleRequest)
      extends GetLifecycleRequestActionCall with LifecycleControlActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        requestid <- exec_from(required_string(action.record, "requestId"))
        requests <- find_lifecycle_requests_all
        request <- exec_from(requests.find(_.requestId.value == requestid).toRight(requestid).fold(Consequence.resourceNotFound, Consequence.success))
        reconciled <- if (Set("queued", "accepted").contains(request.requestState.value)) reconcile_lifecycle_request(request) else exec_pure(request)
      } yield OperationResponse(safe_projection(reconciled))
  }

  private trait LifecycleControlActionSupport { self: ActionCall =>
    protected final def lifecycle_request(actionname: String, record: Record): ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(record, "artifactId"))
        idempotencykey <- exec_from(required_string(record, "idempotencyKey"))
        profileid <- exec_from(lifecycle_launch_profile(actionname, record))
        requests <- find_lifecycle_requests(artifactid)
        response <- requests.find(value => value.lifecycleAction.value == actionname && value.idempotencyKey.value == idempotencykey && value.launchProfileId.map(_.value) == profileid) match {
          case Some(existing) => exec_pure(OperationResponse(safe_projection(existing)))
          case None => _create_lifecycle_request(artifactid, actionname, idempotencykey, profileid)
        }
      } yield response

    private def _create_lifecycle_request(artifactid: String, actionname: String, idempotencykey: String, profileid: Option[String]): ExecUowM[OperationResponse] =
      for {
        components <- find_operational_components(artifactid)
        component <- exec_from(latest_operational_component(components).toRight(artifactid).fold(Consequence.resourceNotFound, Consequence.success))
        now = core.executionContext.clock.instant()
        supervisor <- standalone_supervisor(artifactid, profileid)
        deadlineat = now.plus(Duration.ofSeconds(20))
        queued = component.managementState.value != "excluded" && supervisor.isRight
        state = if (queued) "queued" else "rejected"
        diagnostic = if (queued) None else Some(lifecycle_diagnostic(component, supervisor))
        completedat = if (queued) None else Some(now)
        requestid = core.executionContext.idGeneration.opaqueId("lifecycle-request")
        stored <- entity_create(LifecycleRequestCreate(
          None,
          LifecycleRequestIdValue(requestid),
          ManagedCarArtifactIdValue(artifactid),
          LifecycleActionValue(actionname),
          LifecycleRequestStateValue(state),
          LifecycleIdempotencyKeyValue(idempotencykey),
          now,
          deadlineat,
          None,
          completedat,
          profileid.map(LifecycleLaunchProfileIdValue.apply),
          diagnostic.map(LifecycleDiagnosticCodeValue.apply),
          diagnostic.map(LifecycleDiagnosticValue.apply),
          OperatorSubjectIdValue(executionContext.security.principal.id.value),
          None,
          None
        ))
        request = LifecycleRequestEntity(
          stored.id,
          LifecycleRequestIdValue(requestid),
          ManagedCarArtifactIdValue(artifactid),
          LifecycleActionValue(actionname),
          LifecycleRequestStateValue(state),
          LifecycleIdempotencyKeyValue(idempotencykey),
          now,
          deadlineat,
          None,
          completedat,
          profileid.map(LifecycleLaunchProfileIdValue.apply),
          diagnostic.map(LifecycleDiagnosticCodeValue.apply),
          diagnostic.map(LifecycleDiagnosticValue.apply),
          OperatorSubjectIdValue(executionContext.security.principal.id.value),
          None,
          None
        )
        _ <- if (queued) exec_from(stage_lifecycle_dispatch_event(request.requestId.value)) else exec_pure(())
      } yield OperationResponse(safe_projection(request))

    protected final def dispatch_lifecycle_request(request: LifecycleRequestEntity): ExecUowM[LifecycleRequestEntity] =
      if (request.requestState.value != "queued") exec_pure(request)
      else {
        val now = core.executionContext.clock.instant()
        val protocolrequest = LifecycleSupervisorRequest(
          request.requestId.value,
          request.idempotencyKey.value,
          request.artifactId.value,
          request.launchProfileId.map(_.value),
          request.lifecycleAction.value,
          request.operatorSubjectId.value,
          request.deadlineAt
        )
        for {
          supervisor <- standalone_supervisor(request.artifactId.value, request.launchProfileId.map(_.value))
          result = supervisor.fold(
            code => LifecycleSupervisorProtocol.unavailable(protocolrequest, request.supervisorId.map(_.value).getOrElse(""), code, now),
            _.submit(protocolrequest, now)
          )
          patch <- exec_from(lifecycle_request_update(result))
          _ <- entity_update(request.id, patch)
        } yield lifecycle_request_entity(request, result)
      }

    protected final def reconcile_lifecycle_request(request: LifecycleRequestEntity): ExecUowM[LifecycleRequestEntity] =
      if (!Set("queued", "accepted").contains(request.requestState.value)) exec_pure(request)
      else {
        val now = core.executionContext.clock.instant()
        val protocolrequest = LifecycleSupervisorRequest(request.requestId.value, request.idempotencyKey.value, request.artifactId.value, request.launchProfileId.map(_.value), request.lifecycleAction.value, request.operatorSubjectId.value, request.deadlineAt)
        for {
          supervisor <- standalone_supervisor(request.artifactId.value, request.launchProfileId.map(_.value))
          result = supervisor.fold(
            code => if (request.requestState.value == "queued") Some(LifecycleSupervisorProtocol.unavailable(protocolrequest, request.supervisorId.map(_.value).getOrElse(""), code, now)) else None,
            value => value.lookup(protocolrequest.requestId).orElse(if (request.requestState.value == "queued") Some(value.submit(protocolrequest, now)) else None)
          )
          reconciled <- result match {
            case Some(value) =>
              for {
                patch <- exec_from(lifecycle_request_update(value))
                _ <- entity_update(request.id, patch)
              } yield lifecycle_request_entity(request, value)
            case None => exec_pure(request)
          }
        } yield reconciled
      }

    protected final def standalone_supervisor(artifactId: String, profileId: Option[String]): ExecUowM[Either[String, TextusSupervisor]] =
      for {
        sources <- find_managed_sources_all
      } yield {
        val source = profileId.flatMap { value =>
          value.split(":", 2).toList match {
            case kind :: sourceid :: Nil => latest_sources(sources).find(source => source.artifactId.value == artifactId && source.sourceKind.value == kind && source.sourceId.value == sourceid && source.refreshState.value == "available")
            case _ => None
          }
        }
        val sourcevalidation = profileId match {
          case Some(_) => for {
            _ <- source.toRight("supervisor-launch-profile-unavailable")
            _ <- source.flatMap(value => if (value.sourceKind.value == "DEV") value.privateLocator.map(_.value).filter(_.nonEmpty) else Some("repository-profile")).toRight("supervisor-launch-profile-unavailable")
          } yield ()
          case None => Right(())
        }
        given org.goldenport.cncf.context.ExecutionContext = core.executionContext
        for {
          _ <- sourcevalidation
          _ <- config_string("textus-control-center.home").map(_.trim).filter(_.nonEmpty).toRight("textus-supervisor-home-unavailable")
          socket <- core.component.collect { case value: SupervisorSocket => value }.toRight("textus-supervisor-unavailable")
          provider <- socket.supervisorC.toOption.toRight("textus-supervisor-unavailable")
        } yield new EmbeddedTextusSupervisor(provider, core.executionContext)
      }

    protected final def lifecycle_launch_profile(actionname: String, record: Record): Consequence[Option[String]] =
      if (actionname == "stop") Consequence.success(None)
      else for {
        sourcekind <- required_string(record, "sourceKind")
        sourceid <- required_string(record, "sourceId")
        _ <- if (Set("DEV", "LOCAL", "PUBLIC").contains(sourcekind)) Consequence.unit else Consequence.operationInvalid("sourceKind is invalid")
      } yield Some(s"$sourcekind:$sourceid")

    protected final def lifecycle_request_update(result: LifecycleSupervisorResult): Consequence[LifecycleRequestUpdate] =
      new LifecycleRequestUpdate.Builder()
        .withRequestState(LifecycleRequestStateValue(result.state))
        .withAcceptedAt(result.acceptedAt.fold(Update.setNull[Instant])(Update.set))
        .withCompletedAt(result.completedAt.fold(Update.setNull[Instant])(Update.set))
        .withDiagnosticCode(result.diagnosticCode.map(LifecycleDiagnosticCodeValue.apply).fold(Update.setNull[LifecycleDiagnosticCodeValue])(Update.set))
        .withDiagnostic(result.diagnostic.map(LifecycleDiagnosticValue.apply).fold(Update.setNull[LifecycleDiagnosticValue])(Update.set))
        .withSupervisorId(LifecycleSupervisorIdValue(result.supervisorId))
        .withInstanceId(result.instanceId.map(SubsystemInstanceIdValue.apply).fold(Update.setNull[SubsystemInstanceIdValue])(Update.set))
        .buildC()

    protected final def lifecycle_request_entity(request: LifecycleRequestEntity, result: LifecycleSupervisorResult): LifecycleRequestEntity =
      request.copy(
        requestState = LifecycleRequestStateValue(result.state),
        acceptedAt = result.acceptedAt,
        completedAt = result.completedAt,
        diagnosticCode = result.diagnosticCode.map(LifecycleDiagnosticCodeValue.apply),
        diagnostic = result.diagnostic.map(LifecycleDiagnosticValue.apply),
        supervisorId = Some(result.supervisorId).filter(_.nonEmpty).map(LifecycleSupervisorIdValue.apply),
        instanceId = result.instanceId.map(SubsystemInstanceIdValue.apply)
      )

    protected final def stage_lifecycle_dispatch_event(requestid: String): Consequence[Unit] = {
      val event = ReceptionDomainEvent(
        name = "textus-control-center.lifecycle-request.queued",
        kind = "lifecycle-request",
        payload = Map("requestId" -> requestid),
        attributes = Map("component" -> "textus-control-center"),
        occurredAt = core.executionContext.clock.instant()
      )
      val routableevent = event.copy(attributes = event.attributes.updated(
        org.goldenport.cncf.event.EventReception.StandardAttribute.operationEventTransactionRequirement,
        "ignore"
      ))
      core.executionContext.unitOfWork.stageEvent(routableevent)
      Consequence.unit
    }

    protected final def administrative_principal: Consequence[Unit] = {
      val subject = SecuritySubject.current(using executionContext)
      val privileges = Vector(SecurityContext.Privilege.ApplicationContentManager, SecurityContext.Privilege.Operator, SecurityContext.Privilege.System, SecurityContext.Privilege.Internal)
      if (subject.isAuthenticated && privileges.exists(privilege => subject.hasPrivilege(privilege.name) || subject.hasCapability(privilege.name) || subject.hasRole(privilege.name))) Consequence.unit
      else Consequence.securityPermissionDenied("Lifecycle control requires administrative authorization.")
    }
    protected final def required_string(record: Record, name: String): Consequence[String] =
      record.getString(name).map(_.trim).filter(_.nonEmpty).toRight(s"$name is required").fold(Consequence.operationInvalid, Consequence.success)
    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId.value == artifactid)
    protected final def find_lifecycle_requests(artifactid: String): ExecUowM[Vector[LifecycleRequestEntity]] =
      for { values <- find_lifecycle_requests_all } yield values.filter(_.artifactId.value == artifactid)
    protected final def find_lifecycle_requests_all: ExecUowM[Vector[LifecycleRequestEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "LifecycleRequest")); query = EntityQuery[LifecycleRequestEntity](LifecycleRequestQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[LifecycleRequestEntity](query) } yield result.data
    protected final def find_managed_sources_all: ExecUowM[Vector[ManagedCarSourceEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "ManagedCarSource")); query = EntityQuery[ManagedCarSourceEntity](ManagedCarSourceQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[ManagedCarSourceEntity](query) } yield result.data
    protected final def latest_operational_component(values: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      values.sortBy(value => (value.lastObservedAt, value.id.print)).lastOption
    protected final def latest_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] =
      sources.groupBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value)).valuesIterator.flatMap(_.sortBy(source => (source.snapshotAt, source.id.print)).lastOption).toVector
    protected final def lifecycle_diagnostic(component: OperationalComponentEntity, supervisor: Either[String, TextusSupervisor]): String =
      if (component.managementState.value == "excluded") "component-not-managed"
      else supervisor.fold(identity, _ => "textus-supervisor-unavailable")
    protected final def safe_projection(request: LifecycleRequestEntity): Record = Record.dataAuto(
      "requestId" -> request.requestId.value,
      "artifactId" -> request.artifactId.value,
      "lifecycleAction" -> request.lifecycleAction.value,
      "requestState" -> request.requestState.value,
      "requestedAt" -> request.requestedAt,
      "deadlineAt" -> request.deadlineAt,
      "acceptedAt" -> request.acceptedAt,
      "completedAt" -> request.completedAt,
      "launchProfileId" -> request.launchProfileId.map(_.value),
      "diagnosticCode" -> request.diagnosticCode.map(_.value),
      "diagnostic" -> request.diagnostic.map(_.value),
      "supervisorId" -> request.supervisorId.map(_.value),
      "instanceId" -> request.instanceId.map(_.value)
    )
  }
}
