/*
 *  version Jul. 28, 2026
 * @version Aug. 10, 2026
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
import org.goldenport.cncf.event.{CmlEventCategory, CmlEventDefinition, CmlSubscriptionDefinition, DispatchRoute, EventOriginBoundary, EventReceptionCondition, EventReceptionExecutionPolicy, EventReceptionRule, ReceptionDomainEvent, ReceptionInput, ReceptionOutcome}
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
import org.simplemodeling.textus.controlcenter.catalog.{DevelopmentRoot, LocalRepositoryCatalog, ManagedCar as CatalogManagedCar, ManagedCarCatalog, ManagedCarSource as CatalogManagedCarSource, OperationalComponentManagement, OperationalManagementState, PublicRepositoryCatalog, RuntimeInstance, RuntimeInstanceStatus, StandaloneCatalogConfiguration, StandaloneDevelopmentCatalogProvider, StandaloneLocalRepositoryCatalogProvider, StandalonePublicRepositoryCatalogProvider}
import org.simplemodeling.textus.controlcenter.registry.{RegisteredSubsystem as RegistrySubsystem, RegistryError, RegistrationInput, SubsystemRegistry}
import org.simplemodeling.textus.controlcenter.supervisor.{EmbeddedTextusSupervisor, LifecycleSupervisorProtocol, LifecycleSupervisorRequest, LifecycleSupervisorResult, TextusSupervisor}
import org.simplemodeling.textus.controlcenter.launcher.{LauncherEvidenceClient, LauncherEvidenceClientConfiguration, LauncherEvidenceEntry}

final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    TextusControlCenterPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class TextusControlCenterParticipantFactoryBase extends TextusControlCenterComponent.Factory {
  protected final val shared_services =
    Vector(
      TextusControlCenterComponent.SubsystemInventoryService
      , TextusControlCenterComponent.CarCatalogService
      , TextusControlCenterComponent.OperationalManagementService
      , TextusControlCenterComponent.LauncherEvidenceService
      , TextusControlCenterComponent.LifecycleControlService
    )

  protected final def component_core(
    name: String,
    componentid: ComponentId
  ): Component.Core =
    spec_create(name, componentid, shared_services)

  override val SubsystemInventory: TextusControlCenterComponent.SubsystemInventoryServiceFactory =
    SubsystemInventoryServiceFactoryImpl()
  override val CarCatalog: TextusControlCenterComponent.CarCatalogServiceFactory =
    CarCatalogServiceFactoryImpl()
  override val OperationalManagement: TextusControlCenterComponent.OperationalManagementServiceFactory =
    OperationalManagementServiceFactoryImpl()
  override val LauncherEvidence: TextusControlCenterComponent.LauncherEvidenceServiceFactory =
    LauncherEvidenceServiceFactoryImpl()
  override val LifecycleControl: TextusControlCenterComponent.LifecycleControlServiceFactory =
    LifecycleControlServiceFactoryImpl()
  override val aggregate: TextusControlCenterComponent.AggregateServiceFactory =
    AggregateServiceFactoryImpl()
  override val view: TextusControlCenterComponent.ViewServiceFactory =
    ViewServiceFactoryImpl()
  override val entity: TextusControlCenterComponent.EntityServiceFactory =
    EntityServiceFactoryImpl()
}

final class TextusControlCenterPrimaryComponent(
  registrationauthentication: AuthenticationProvider
) extends TextusControlCenterComponent with SupervisorSocket {
  override def authenticationProviders: Vector[AuthenticationProvider] =
    Vector(registrationauthentication)

  override def eventReceptionDefinitions: Vector[CmlEventDefinition] =
    Vector(CmlEventDefinition("textus-control-center.lifecycle-request.queued", CmlEventCategory.NonActionEvent))

  override def eventSubscriptionDefinitions: Vector[CmlSubscriptionDefinition] =
    Vector(CmlSubscriptionDefinition(
      name = "dispatch-queued-lifecycle-request",
      eventName = "textus-control-center.lifecycle-request.queued",
      route = DispatchRoute.Broadcast,
      actionName = "lifecycle-control.dispatch-lifecycle-request"
    ))

  override def eventReceptionRuleDefinitions: Vector[EventReceptionRule] =
    eventReceptionDefinitions.map { definition =>
      EventReceptionRule(
        name = s"textus-control-center-post-commit-${definition.name.replace('.', '-')}",
        condition = EventReceptionCondition(
          originBoundary = Some(EventOriginBoundary.SameSubsystem),
          eventName = Some(definition.name)
        ),
        policy = EventReceptionExecutionPolicy.AsyncNewJobSameSaga
      )
    }
}

object TextusControlCenterPrimaryFactory extends TextusControlCenterParticipantFactoryBase with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component =
    new TextusControlCenterPrimaryComponent(
      TextusControlCenterLauncherRegistrationAuthenticationProvider.fromConfiguration(
        params.configuration
      )
    )

  override protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core =
    component_core(TextusControlCenterComponent.name, TextusControlCenterComponent.componentId)
}

final class SubsystemInventoryServiceFactoryImpl extends TextusControlCenterComponent.SubsystemInventoryServiceFactory {
  import TextusControlCenterComponent.SubsystemInventoryService.*

  override def createRegisterSubsystemActionCall(
    core: ActionCall.Core,
    action: RegisterSubsystem
  ): RegisterSubsystemActionCall =
    RegisterSubsystemActionCallImpl(core, action)

  override def createHeartbeatSubsystemActionCall(
    core: ActionCall.Core,
    action: HeartbeatSubsystem
  ): HeartbeatSubsystemActionCall =
    HeartbeatSubsystemActionCallImpl(core, action)

  override def createDeregisterSubsystemActionCall(
    core: ActionCall.Core,
    action: DeregisterSubsystem
  ): DeregisterSubsystemActionCall =
    DeregisterSubsystemActionCallImpl(core, action)

  override def createListSubsystemsActionCall(
    core: ActionCall.Core,
    action: ListSubsystems
  ): ListSubsystemsActionCall =
    ListSubsystemsActionCallImpl(core, action)

  override def createGetSubsystemActionCall(
    core: ActionCall.Core,
    action: GetSubsystem
  ): GetSubsystemActionCall =
    GetSubsystemActionCallImpl(core, action)

  private final case class RegisterSubsystemActionCallImpl(
    core: ActionCall.Core,
    override val action: RegisterSubsystem
  ) extends RegisterSubsystemActionCall with RegistryActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        input <- exec_from(registration_input(action.record))
        principal <- exec_from(registration_principal)
        matches <- find_registered(input.instanceId)
        current <- exec_from(single_registered(input.instanceId, matches))
        now = core.executionContext.clock.instant()
        next <- exec_from(SubsystemRegistry.register(current.map(to_registry), input, principal, now).fold(registry_error, Consequence.success))
        stored <- persist(next)
        _ <- ensure_adopted_operational_component(stored.artifactId.map(_.value), now)
      } yield OperationResponse(safe_projection(to_registry(stored), now))
  }

  private final case class HeartbeatSubsystemActionCallImpl(
    core: ActionCall.Core,
    override val action: HeartbeatSubsystem
  ) extends HeartbeatSubsystemActionCall with RegistryActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        input <- exec_from(registration_input(action.record))
        principal <- exec_from(registration_principal)
        matches <- find_registered(input.instanceId)
        current <- exec_from(single_registered(input.instanceId, matches))
        now = core.executionContext.clock.instant()
        next <- exec_from(SubsystemRegistry.heartbeat(current.map(to_registry), input, principal, now).fold(registry_error, Consequence.success))
        stored <- persist(next)
      } yield OperationResponse(safe_projection(to_registry(stored), now))
  }

  private final case class DeregisterSubsystemActionCallImpl(
    core: ActionCall.Core,
    override val action: DeregisterSubsystem
  ) extends DeregisterSubsystemActionCall with RegistryActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        instanceid <- exec_from(required_string(action.record, "instanceId"))
        principal <- exec_from(registration_principal)
        matches <- find_registered(instanceid)
        current <- exec_from(single_registered(instanceid, matches))
        now = core.executionContext.clock.instant()
        next <- exec_from(SubsystemRegistry.deregister(current.map(to_registry), instanceid, principal, now).fold(registry_error, Consequence.success))
        stored <- persist(next)
      } yield OperationResponse(safe_projection(to_registry(stored), now))
  }

  private final case class ListSubsystemsActionCallImpl(
    core: ActionCall.Core,
    override val action: ListSubsystems
  ) extends ListSubsystemsActionCall with RegistryActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        all <- find_registered_all
        now = core.executionContext.clock.instant()
        text = action.record.getString("text").map(_.trim.toLowerCase).filter(_.nonEmpty)
        offset = action.record.getInt("offset").getOrElse(0).max(0)
        limit = action.record.getInt("limit").getOrElse(100).max(0)
        filtered = SubsystemRegistry.ordered(latest_registered(all).map(to_registry)).filter(source => text.forall(matches_text(source, _)))
        page = filtered.drop(offset).take(limit)
      } yield OperationResponse(Record.dataAuto(
        "data" -> page.map(source => safe_projection(source, now)),
        "totalCount" -> filtered.size,
        "offset" -> offset,
        "limit" -> limit
      ))
  }

  private final case class GetSubsystemActionCallImpl(
    core: ActionCall.Core,
    override val action: GetSubsystem
  ) extends GetSubsystemActionCall with RegistryActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        instanceid <- exec_from(required_string(action.record, "instanceId"))
        matches <- find_registered(instanceid)
        current <- exec_from(single_registered(instanceid, matches))
        entity <- exec_from(current.toRight(RegistryError.Missing(instanceid)).fold(registry_error, Consequence.success))
        now = core.executionContext.clock.instant()
      } yield OperationResponse(safe_projection(to_registry(entity), now))
  }

  private trait RegistryActionSupport { self: ActionCall =>
    protected final val STALE_THRESHOLD: Duration = Duration.ofSeconds(90)

    protected final def registration_input(record: Record): Consequence[RegistrationInput] = {
      for {
        protocolversion <- record.getInt("protocolVersion").toRight("protocolVersion is required").fold(Consequence.operationInvalid, Consequence.success)
        instanceid <- required_string(record, "instanceId")
        launcherkind <- required_string(record, "launcherKind")
        target <- required_string(record, "target")
        baseurl <- required_string(record, "baseUrl")
        hostlabel <- required_string(record, "hostLabel")
        startedat <- record.getAny("startedAt") match {
          case Some(value: Instant) => Consequence.success(value)
          case Some(value) => scala.util.Try(Instant.parse(value.toString)).toOption.toRight("startedAt must be an ISO-8601 instant").fold(Consequence.operationInvalid, Consequence.success)
          case None => Consequence.operationInvalid("startedAt is required")
        }
        launcherstate <- required_string(record, "launcherState")
      } yield RegistrationInput(
        protocolversion,
        instanceid,
        launcherkind,
        target,
        record.getString("artifactId").map(_.trim).filter(_.nonEmpty),
        record.getString("executionMode").map(_.trim).filter(_.nonEmpty),
        record.getString("developmentDirectory").map(_.trim).filter(_.nonEmpty),
        record.getString("subsystemName").map(_.trim).filter(_.nonEmpty),
        record.getString("subsystemVersion").map(_.trim).filter(_.nonEmpty),
        record.getString("runtimeVersion").map(_.trim).filter(_.nonEmpty),
        baseurl,
        hostlabel,
        startedat,
        launcherstate
      )
    }

    protected final def registration_principal: Consequence[String] = {
      val subject = SecuritySubject.current(using executionContext)
      if (subject.isAuthenticated && executionContext.security.hasCapability(TextusControlCenterLauncherRegistrationAuthenticationProvider.CAPABILITY))
        Consequence.success(executionContext.security.principal.id.value)
      else Consequence.securityAuthenticationRequired("Subsystem registration requires an authenticated launcher principal.")
    }

    protected final def administrative_principal: Consequence[Unit] = {
      val subject = SecuritySubject.current(using executionContext)
      val privileges = Vector(
        SecurityContext.Privilege.ApplicationContentManager,
        SecurityContext.Privilege.Operator,
        SecurityContext.Privilege.System,
        SecurityContext.Privilege.Internal
      )
      if (subject.isAuthenticated && privileges.exists { privilege =>
        subject.hasPrivilege(privilege.name) ||
        subject.hasCapability(privilege.name) ||
        subject.hasRole(privilege.name)
      })
        Consequence.unit
      else Consequence.securityPermissionDenied("Subsystem inventory requires administrative authorization.")
    }

    protected final def required_string(record: Record, name: String): Consequence[String] =
      record.getString(name).map(_.trim).filter(_.nonEmpty).toRight(s"$name is required").fold(Consequence.operationInvalid, Consequence.success)

    protected final def find_registered(instanceid: String): ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for {
        fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem"))
        query = EntityQuery[RegisteredSubsystemEntity](
          RegisteredSubsystemQuery.collectionId,
          fields.rewrite(Query.fromRecord(Record.dataAuto("instanceId" -> instanceid))),
          scope = EntitySearchScope.Store,
          visibilityScope = Some(EntityVisibilityScope.Public)
        )
        result <- entity_search_internal[RegisteredSubsystemEntity](query)
    } yield result.data.filter(_.instanceId.value == instanceid)

    protected final def find_registered_all: ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for {
        fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem"))
        query = EntityQuery[RegisteredSubsystemEntity](
          RegisteredSubsystemQuery.collectionId,
          fields.rewrite(Query.fromRecord(Record.empty)),
          scope = EntitySearchScope.Store,
          visibilityScope = Some(EntityVisibilityScope.Admin)
        )
        result <- entity_search_internal[RegisteredSubsystemEntity](query)
      } yield result.data

    protected final def single_registered(
      instanceid: String,
      sources: Vector[RegisteredSubsystemEntity]
    ): Consequence[Option[RegisteredSubsystemEntity]] =
      Consequence.success(sources.sortBy(source => (source.lastSeenAt, source.id.print)).lastOption)

    protected final def latest_registered(
      sources: Vector[RegisteredSubsystemEntity]
    ): Vector[RegisteredSubsystemEntity] =
      sources.groupBy(_.instanceId).valuesIterator.flatMap(_.sortBy(source => (source.lastSeenAt, source.id.print)).lastOption).toVector

    protected final def to_registry(source: RegisteredSubsystemEntity): RegistrySubsystem =
      RegistrySubsystem(
        source.protocolVersion,
        source.instanceId.value,
        source.launcherKind.value,
        source.target.value,
        source.artifactId.map(_.value),
        source.executionMode.map(_.value),
        source.developmentDirectory.map(_.value),
        source.subsystemName.map(_.value),
        source.subsystemVersion.map(_.value),
        source.runtimeVersion.map(_.value),
        source.baseUrl.toExternalForm,
        source.hostLabel.toI18nString.displayMessage,
        source.startedAt,
        source.lastSeenAt,
        source.launcherState.value,
        source.registrationPrincipalId.value
      )

    protected final def to_create(
      source: RegistrySubsystem
    ): Consequence[RegisteredSubsystemCreate] =
      base_url(source.baseUrl).map { baseurl =>
        RegisteredSubsystemCreate(
          None,
          SubsystemInstanceIdValue(source.instanceId),
          source.protocolVersion,
          LauncherKindValue(source.launcherKind),
          SubsystemTargetValue(source.target),
          source.artifactId.map(ManagedCarArtifactIdValue.apply),
          source.executionMode.map(ExecutionModeValue.apply),
          source.developmentDirectory.map(DevelopmentDirectoryValue.apply),
          source.subsystemName.map(SubsystemNameValue.apply),
          source.subsystemVersion.map(SubsystemVersionValue.apply),
          source.runtimeVersion.map(RuntimeVersionValue.apply),
          baseurl,
          I18nLabel(source.hostLabel),
          source.startedAt,
          source.lastSeenAt,
          LauncherStateValue(source.launcherState),
          RegistrationPrincipalIdValue(source.registrationPrincipalId)
        )
      }

    protected final def persist(source: RegistrySubsystem): ExecUowM[RegisteredSubsystemEntity] =
      for {
        create <- exec_from(to_create(source))
        result <- entity_create(create)
      } yield RegisteredSubsystemEntity(
        result.id,
        SubsystemInstanceIdValue(source.instanceId),
        source.protocolVersion,
        LauncherKindValue(source.launcherKind),
        SubsystemTargetValue(source.target),
        source.artifactId.map(ManagedCarArtifactIdValue.apply),
        source.executionMode.map(ExecutionModeValue.apply),
        source.developmentDirectory.map(DevelopmentDirectoryValue.apply),
        source.subsystemName.map(SubsystemNameValue.apply),
        source.subsystemVersion.map(SubsystemVersionValue.apply),
        source.runtimeVersion.map(RuntimeVersionValue.apply),
        create.baseUrl,
        create.hostLabel,
        source.startedAt,
        source.lastSeenAt,
        LauncherStateValue(source.launcherState),
        RegistrationPrincipalIdValue(source.registrationPrincipalId)
      )

    protected final def base_url(value: String): Consequence[URL] =
      try {
        val uri = URI.create(value)
        if (Option(uri.getScheme).exists(value => Set("http", "https").contains(value.toLowerCase)) && Option(uri.getHost).exists(_.nonEmpty)) Consequence.success(uri.toURL)
        else Consequence.valueInvalid(s"baseUrl is invalid: $value", XString)
      } catch {
        case NonFatal(_) => Consequence.valueInvalid(s"baseUrl is invalid: $value", XString)
      }

    protected final def ensure_adopted_operational_component(artifactid: Option[String], now: Instant): ExecUowM[Unit] =
      artifactid match {
        case Some(value) =>
          for {
            existing <- find_operational_components(value)
            _ <- latest_operational_component(existing) match {
              case Some(_) => exec_from(Consequence.unit)
        case None => entity_create(OperationalComponentCreate(None, ManagedCarArtifactIdValue(value), OperationalManagementStateValue("adopted"), now, now)).map(_ => ())
            }
          } yield ()
        case None => exec_from(Consequence.unit)
      }

    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for {
        fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent"))
        query = EntityQuery[OperationalComponentEntity](
          OperationalComponentQuery.collectionId,
          fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))),
          scope = EntitySearchScope.Store,
          visibilityScope = Some(EntityVisibilityScope.Admin)
        )
        result <- entity_search_internal[OperationalComponentEntity](query)
  } yield result.data.filter(_.artifactId.value == artifactid)

    protected final def latest_operational_component(sources: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      sources.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption

    protected final def safe_projection(source: RegistrySubsystem, now: Instant): Record =
      SubsystemRegistry.projection(source, now, STALE_THRESHOLD) match {
        case Right(projection) => Record.dataAuto(
          "protocolVersion" -> projection.protocolVersion,
          "instanceId" -> projection.instanceId,
          "launcherKind" -> projection.launcherKind,
          "target" -> projection.target,
          "artifactId" -> projection.artifactId,
          "executionMode" -> projection.executionMode,
          "developmentDirectory" -> projection.developmentDirectory,
          "subsystemName" -> projection.subsystemName,
          "subsystemVersion" -> projection.subsystemVersion,
          "runtimeVersion" -> projection.runtimeVersion,
          "baseUrl" -> projection.baseUrl,
          "hostLabel" -> projection.hostLabel,
          "startedAt" -> projection.startedAt,
          "lastSeenAt" -> projection.lastSeenAt,
          "launcherState" -> projection.launcherState,
          "status" -> projection.status,
          "dashboardUrl" -> projection.dashboardUrl,
          "systemAdminUrl" -> projection.systemAdminUrl
        )
        case Left(error) => throw new IllegalStateException(error.message)
      }

    protected final def matches_text(source: RegistrySubsystem, text: String): Boolean =
      Vector(source.instanceId, source.launcherKind, source.target, source.hostLabel)
        .concat(source.executionMode.toVector)
        .concat(source.subsystemName.toVector)
        .exists(_.toLowerCase.contains(text))

    protected final def registry_error(error: RegistryError): Consequence[Nothing] = error match {
      case RegistryError.Invalid(message) => Consequence.operationInvalid(message)
      case RegistryError.Missing(_) => Consequence.resourceNotFound(error.message)
      case RegistryError.Conflict(_, _) => Consequence.stateConflict(error.message)
      case RegistryError.Unauthorized(_) => Consequence.securityPermissionDenied(error.message)
    }
  }
}

final class CarCatalogServiceFactoryImpl extends TextusControlCenterComponent.CarCatalogServiceFactory {
  import TextusControlCenterComponent.CarCatalogService.*

  override def createRefreshCarCatalogActionCall(core: ActionCall.Core, action: RefreshCarCatalog): RefreshCarCatalogActionCall =
    RefreshCarCatalogActionCallImpl(core, action)
  override def createListManagedCarsActionCall(core: ActionCall.Core, action: ListManagedCars): ListManagedCarsActionCall =
    ListManagedCarsActionCallImpl(core, action)
  override def createGetManagedCarActionCall(core: ActionCall.Core, action: GetManagedCar): GetManagedCarActionCall =
    GetManagedCarActionCallImpl(core, action)

  private final case class RefreshCarCatalogActionCallImpl(core: ActionCall.Core, override val action: RefreshCarCatalog)
      extends RefreshCarCatalogActionCall with CatalogActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        now = core.executionContext.clock.instant()
        catalogfile = StandaloneCatalogConfiguration.configuredFile(config_string("textus-control-center.catalog.file"), config_string("textus-control-center.home"))
        catalogconfiguration = catalogfile.flatMap(path => StandaloneCatalogConfiguration.load(path).toOption)
        developmentroots = catalogconfiguration.map(_.developmentRoots).getOrElse(
          config_string("textus-control-center.catalog.development.root").map(_.trim).filter(_.nonEmpty).map(path => DevelopmentRoot("standalone-development", path)).toVector
        )
        localcatalogs = catalogconfiguration.flatMap(_.localRepositoryCatalog).toVector ++
          (if (catalogconfiguration.isDefined) Vector.empty else config_string("textus-control-center.catalog.local-repository.catalog-root").map(_.trim).filter(_.nonEmpty).map(path => LocalRepositoryCatalog("standalone-local-repository", path)).toVector)
        publicrepositories = catalogconfiguration.map(_.publicRepositoryCatalogs).getOrElse {
          val publicbase = config_string("textus-control-center.catalog.public.base-url").map(_.trim).filter(_.nonEmpty)
          val publicsubscriptions = config_string("textus-control-center.catalog.public.artifact-ids").toVector.flatMap(_.split(',').toVector.map(_.trim).filter(_.nonEmpty)).distinct
          publicbase.filter(_ => publicsubscriptions.nonEmpty).map(base => PublicRepositoryCatalog("simplemodeling-public", base, publicsubscriptions)).toVector
        }
        refreshtimeout = catalogconfiguration.map(_.refreshTimeout).getOrElse(StandaloneCatalogConfiguration.DEFAULT_REFRESH_TIMEOUT)
        existingcars <- find_managed_cars_all
        existingsources <- find_managed_sources_all
        local = developmentroots.flatMap(StandaloneDevelopmentCatalogProvider.discover(_, now)) ++ localcatalogs.flatMap(StandaloneLocalRepositoryCatalogProvider.discover(_, now))
        publicrefreshes <- publicrepositories.traverse(fetch_public_sources(_, now, refreshtimeout))
        public = publicrefreshes.flatMap(_._1)
        refreshdiagnostics = publicrefreshes.flatMap(_._2)
        discovered = local ++ public
        stored <- discovered.traverse(persist_discovered_source(_, now, existingcars, existingsources))
      } yield OperationResponse(Record.dataAuto(
        "artifactId" -> action.record.getString("artifactId").map(_.trim).filter(_.nonEmpty),
        "refreshedAt" -> now,
        "refreshedSourceCount" -> stored.size,
        "adapterState" -> (if (catalogfile.isDefined && catalogconfiguration.isEmpty) "invalid" else if (developmentroots.nonEmpty || localcatalogs.nonEmpty || publicrepositories.nonEmpty) "configured" else "not-configured"),
        "refreshDiagnostics" -> refreshdiagnostics
      ))
  }

  private final case class ListManagedCarsActionCallImpl(core: ActionCall.Core, override val action: ListManagedCars)
      extends ListManagedCarsActionCall with CatalogActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        cars <- find_managed_cars_all
        sources <- find_managed_sources_all
        registered <- find_registered_subsystems_all
        now = core.executionContext.clock.instant()
        text = action.record.getString("text").map(_.trim.toLowerCase).filter(_.nonEmpty)
        offset = action.record.getInt("offset").getOrElse(0).max(0)
        limit = action.record.getInt("limit").getOrElse(100).max(0)
        filtered = latest_cars(cars).filter(car => text.forall(value => matches_text(car, value)))
        page = filtered.drop(offset).take(limit)
      } yield OperationResponse(Record.dataAuto(
        "data" -> page.map(car => safe_car_projection(car, latest_sources(sources).filter(_.artifactId == car.artifactId), false, runtime_summary(car, latest_cars(cars), registered, now))),
        "totalCount" -> filtered.size,
        "offset" -> offset,
        "limit" -> limit
      ))
  }

  private final case class GetManagedCarActionCallImpl(core: ActionCall.Core, override val action: GetManagedCar)
      extends GetManagedCarActionCall with CatalogActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(action.record, "artifactId"))
        cars <- find_managed_cars_all
        car <- exec_from(latest_cars(cars).find(_.artifactId.value == artifactid).toRight(RegistryError.Missing(artifactid)).fold(registry_error, Consequence.success))
        sources <- find_managed_sources_all
        registered <- find_registered_subsystems_all
        now = core.executionContext.clock.instant()
    } yield OperationResponse(safe_car_projection(car, latest_sources(sources).filter(_.artifactId.value == artifactid), true, runtime_summary(car, latest_cars(cars), registered, now)))
  }

  private trait CatalogActionSupport { self: ActionCall =>
    protected final def administrative_principal: Consequence[Unit] = {
      val subject = SecuritySubject.current(using executionContext)
      val privileges = Vector(SecurityContext.Privilege.ApplicationContentManager, SecurityContext.Privilege.Operator, SecurityContext.Privilege.System, SecurityContext.Privilege.Internal)
      if (subject.isAuthenticated && privileges.exists(privilege => subject.hasPrivilege(privilege.name) || subject.hasCapability(privilege.name) || subject.hasRole(privilege.name))) Consequence.unit
      else Consequence.securityPermissionDenied("CAR catalog requires administrative authorization.")
    }
    protected final def required_string(record: Record, name: String): Consequence[String] =
      record.getString(name).map(_.trim).filter(_.nonEmpty).toRight(s"$name is required").fold(Consequence.operationInvalid, Consequence.success)
    protected final def registry_error(error: RegistryError): Consequence[Nothing] = error match {
      case RegistryError.Missing(_) => Consequence.resourceNotFound(error.message)
      case _ => Consequence.operationInvalid(error.message)
    }
    protected final def find_managed_cars_all: ExecUowM[Vector[ManagedCarEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "ManagedCar")); query = EntityQuery[ManagedCarEntity](ManagedCarQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[ManagedCarEntity](query) } yield result.data
    protected final def find_managed_sources_all: ExecUowM[Vector[ManagedCarSourceEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "ManagedCarSource")); query = EntityQuery[ManagedCarSourceEntity](ManagedCarSourceQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[ManagedCarSourceEntity](query) } yield result.data
    protected final def find_registered_subsystems_all: ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem")); query = EntityQuery[RegisteredSubsystemEntity](RegisteredSubsystemQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[RegisteredSubsystemEntity](query) } yield result.data
    protected final def fetch_public_sources(repository: PublicRepositoryCatalog, now: Instant, timeout: Duration): ExecUowM[(Vector[CatalogManagedCarSource], Vector[String])] =
      def fetch(artifactids: Vector[String]): ExecUowM[Vector[CatalogManagedCarSource]] = artifactids.traverse { artifactid =>
        http_get(
          StandalonePublicRepositoryCatalogProvider.catalog_url(repository, artifactid),
          Map("Accept" -> "application/yaml, text/yaml"),
          Vector(Property("http.timeout-seconds", timeout.toSeconds.toString, None))
        )
          .map { response =>
            if (response.status.code / 100 == 2)
              StandalonePublicRepositoryCatalogProvider.available(repository, artifactid, response.getString.getOrElse(""), now)
            else
              StandalonePublicRepositoryCatalogProvider.unavailable(repository, artifactid, now)
          }
      }
      if (repository.subscriptions.nonEmpty)
        fetch(repository.subscriptions.distinct.sorted).map(_ -> Vector.empty)
      else
        http_get(
          StandalonePublicRepositoryCatalogProvider.indexUrl(repository),
          Map("Accept" -> "application/json"),
          Vector(Property("http.timeout-seconds", timeout.toSeconds.toString, None))
        ).flatMap { response =>
          if (response.status.code / 100 == 2)
            StandalonePublicRepositoryCatalogProvider.indexArtifactIds(response.getString.getOrElse("")).fold(
              _ => exec_pure(Vector.empty[CatalogManagedCarSource] -> Vector("public-index-invalid")),
              ids => fetch(ids).map(_ -> Vector.empty)
            )
          else
            exec_pure(Vector.empty[CatalogManagedCarSource] -> Vector("public-index-unavailable"))
        }
    protected final def persist_discovered_source(
      source: CatalogManagedCarSource,
      now: Instant,
      existingcars: Vector[ManagedCarEntity],
      existingsources: Vector[ManagedCarSourceEntity]
    ): ExecUowM[ManagedCarSourceEntity] =
      for {
        _ <- ensure_managed_car(source, existingcars, now)
        _ <- ensure_auto_managed_operational_component(source, now)
        retained = retain_source_facts(source, existingsources)
        recommended = retained.availableVersions.headOption
        latest = retained.availableVersions.lastOption
        stored <- entity_create(ManagedCarSourceCreate(None, ManagedCarArtifactIdValue(retained.artifactId), ManagedCarSourceIdValue(retained.sourceId), ManagedCarSourceKindValue(retained.sourceKind.mark), ManagedCarRefreshStateValue(retained.refreshState.toString.toLowerCase), retained.componentName.map(ManagedCarComponentNameValue.apply), recommended.map(ManagedCarVersionValue.apply), latest.map(ManagedCarVersionValue.apply), retained.snapshotAt, retained.diagnostic.map(ManagedCarDiagnosticValue.apply), retained.privateLocator.map(ManagedCarPrivateLocatorValue.apply)))
      } yield ManagedCarSourceEntity(stored.id, ManagedCarArtifactIdValue(retained.artifactId), ManagedCarSourceIdValue(retained.sourceId), ManagedCarSourceKindValue(retained.sourceKind.mark), ManagedCarRefreshStateValue(retained.refreshState.toString.toLowerCase), retained.componentName.map(ManagedCarComponentNameValue.apply), recommended.map(ManagedCarVersionValue.apply), latest.map(ManagedCarVersionValue.apply), retained.snapshotAt, retained.diagnostic.map(ManagedCarDiagnosticValue.apply), retained.privateLocator.map(ManagedCarPrivateLocatorValue.apply))
    protected final def ensure_managed_car(source: CatalogManagedCarSource, existing: Vector[ManagedCarEntity], now: Instant): ExecUowM[Unit] =
      existing.find(_.artifactId.value == source.artifactId) match {
        case Some(car) =>
          for {
            patch <- exec_from(managed_car_update(source.componentName.orElse(car.componentName.map(_.value)), now))
            _ <- entity_update(car.id, patch)
          } yield ()
        case None => entity_create(ManagedCarCreate(None, ManagedCarArtifactIdValue(source.artifactId), source.componentName.map(ManagedCarComponentNameValue.apply), now, now)).map(_ => ())
      }
    protected final def ensure_auto_managed_operational_component(source: CatalogManagedCarSource, now: Instant): ExecUowM[Unit] =
      if (source.sourceKind == org.simplemodeling.textus.controlcenter.catalog.ManagedCarSourceKind.Development) {
        for {
          existing <- find_operational_components(source.artifactId)
          _ <- latest_operational_component(existing) match {
            case Some(component) if component.managementState.value == "excluded" => exec_from(Consequence.unit)
            case Some(component) =>
              for {
                patch <- exec_from(operational_component_update("auto-managed", now))
                _ <- entity_update(component.id, patch)
              } yield ()
            case None => entity_create(OperationalComponentCreate(None, ManagedCarArtifactIdValue(source.artifactId), OperationalManagementStateValue("auto-managed"), now, now)).map(_ => ())
          }
        } yield ()
      } else {
        exec_from(Consequence.unit)
      }
    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId.value == artifactid)
    protected final def latest_operational_component(sources: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      sources.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption
    protected final def managed_car_update(componentname: Option[String], now: Instant): Consequence[ManagedCarUpdate] =
      componentname match {
        case Some(value) => new ManagedCarUpdate.Builder().withComponentName(ManagedCarComponentNameValue(value)).withLastObservedAt(now).buildC()
        case None => new ManagedCarUpdate.Builder().withLastObservedAt(now).buildC()
      }
    protected final def operational_component_update(managementstate: String, now: Instant): Consequence[OperationalComponentUpdate] =
      new OperationalComponentUpdate.Builder().withManagementState(OperationalManagementStateValue(managementstate)).withLastObservedAt(now).buildC()
    protected final def retain_source_facts(source: CatalogManagedCarSource, existing: Vector[ManagedCarSourceEntity]): CatalogManagedCarSource =
      if (source.refreshState == org.simplemodeling.textus.controlcenter.catalog.ManagedCarRefreshState.Available) source
      else latest_sources(existing).find(current => current.artifactId.value == source.artifactId && current.sourceKind.value == source.sourceKind.mark && current.sourceId.value == source.sourceId) match {
        case Some(current) => source.copy(
          componentName = source.componentName.orElse(current.componentName.map(_.value)),
          availableVersions = if (source.availableVersions.nonEmpty) source.availableVersions else Vector(current.recommendedVersion, current.latestVersion).flatten.map(_.value).distinct
        )
        case None => source
      }
    protected final def latest_cars(sources: Vector[ManagedCarEntity]): Vector[ManagedCarEntity] =
      sources.groupBy(_.artifactId.value).valuesIterator.flatMap(_.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption).toVector.sortBy(_.artifactId.value)
    protected final def latest_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] =
      sources.groupBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value)).valuesIterator.flatMap(_.sortBy(source => (source.snapshotAt, source.id.print)).lastOption).toVector.sortBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value))
    protected final def latest_registered_subsystems(sources: Vector[RegisteredSubsystemEntity]): Vector[RegisteredSubsystemEntity] =
      sources.groupBy(_.instanceId.value).valuesIterator.flatMap(_.sortBy(source => (source.lastSeenAt, source.id.print)).lastOption).toVector.sortBy(_.instanceId.value)
    protected final def runtime_summary(car: ManagedCarEntity, cars: Vector[ManagedCarEntity], registered: Vector[RegisteredSubsystemEntity], now: Instant) = {
      val catalogcars = cars.map(source => CatalogManagedCar(source.artifactId.value, source.componentName.map(_.value), source.componentName.map(_.value).toSet, Vector.empty, Vector.empty))
      val instances = latest_registered_subsystems(registered).map { source =>
        val registry = RegistrySubsystem(source.protocolVersion, source.instanceId.value, source.launcherKind.value, source.target.value, source.artifactId.map(_.value), source.executionMode.map(_.value), source.developmentDirectory.map(_.value), source.subsystemName.map(_.value), source.subsystemVersion.map(_.value), source.runtimeVersion.map(_.value), source.baseUrl.toExternalForm, source.hostLabel.toI18nString.displayMessage, source.startedAt, source.lastSeenAt, source.launcherState.value, source.registrationPrincipalId.value)
        val status = SubsystemRegistry.projection(registry, now, Duration.ofSeconds(90)).toOption.map(_.status) match {
          case Some(SubsystemRegistry.running) => RuntimeInstanceStatus.Running
          case Some(SubsystemRegistry.starting) => RuntimeInstanceStatus.Starting
          case Some(SubsystemRegistry.stale) => RuntimeInstanceStatus.Stale
          case _ => RuntimeInstanceStatus.Stopped
        }
        RuntimeInstance(source.instanceId.value, source.artifactId.map(_.value), source.target.value, source.subsystemName.map(_.value), status)
      }
      ManagedCarCatalog.runtimeSummary(car.artifactId.value, instances, ManagedCarCatalog.linkRuntimeInstances(catalogcars, instances))
    }
    protected final def safe_car_projection(car: ManagedCarEntity, sources: Vector[ManagedCarSourceEntity], detail: Boolean, runtime: org.simplemodeling.textus.controlcenter.catalog.ManagedCarRuntimeSummary): Record =
      Record.dataAuto("artifactId" -> car.artifactId.value, "componentName" -> car.componentName.map(_.value), "createdAt" -> car.firstObservedAt, "updatedAt" -> car.lastObservedAt, "runtimeState" -> (runtime.state match { case org.simplemodeling.textus.controlcenter.catalog.ManagedCarRuntimeState.NotRunning => "not-running"; case state => state.toString.toLowerCase }), "activeInstanceIds" -> runtime.activeInstanceIds, "staleInstanceIds" -> runtime.staleInstanceIds, "sources" -> sources.map(source => Record.dataAuto("sourceId" -> source.sourceId.value, "sourceKind" -> source.sourceKind.value, "refreshState" -> source.refreshState.value, "componentName" -> source.componentName.map(_.value), "recommendedVersion" -> source.recommendedVersion.map(_.value), "latestVersion" -> source.latestVersion.map(_.value), "snapshotAt" -> source.snapshotAt, "diagnostic" -> source.diagnostic.map(_.value), "privateLocator" -> (if (detail) source.privateLocator.map(_.value) else None))))
    protected final def matches_text(car: ManagedCarEntity, text: String): Boolean = Vector(car.artifactId.value).concat(car.componentName.map(_.value).toVector).exists(_.toLowerCase.contains(text))
  }
}

object SubsystemInventoryServiceFactoryImpl {
  def apply(): SubsystemInventoryServiceFactoryImpl = new SubsystemInventoryServiceFactoryImpl()
}

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
    protected final def latest_operational_components(sources: Vector[OperationalComponentEntity]): Vector[OperationalComponentEntity] = sources.groupBy(_.artifactId.value).valuesIterator.flatMap(latest_operational_component).toVector.sortBy(_.artifactId.value)
    protected final def latest_managed_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] = sources.groupBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value)).valuesIterator.flatMap(source => source.sortBy(value => (value.snapshotAt, value.id.print)).lastOption).toVector
    protected final def operational_component_update(managementstate: String, now: Instant): Consequence[OperationalComponentUpdate] =
      new OperationalComponentUpdate.Builder().withManagementState(OperationalManagementStateValue(managementstate)).withLastObservedAt(now).buildC()
    protected final def safe_projection(component: OperationalComponentEntity): Record = Record.dataAuto("artifactId" -> component.artifactId.value, "managementState" -> component.managementState.value, "firstManagedAt" -> component.firstManagedAt, "lastObservedAt" -> component.lastObservedAt)
  }
}

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
        reconciled <- if (request.requestState.value == "queued") reconcile_lifecycle_request(request) else exec_pure(request)
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
      if (request.requestState.value != "queued") exec_pure(request)
      else {
        val now = core.executionContext.clock.instant()
        val protocolrequest = LifecycleSupervisorRequest(request.requestId.value, request.idempotencyKey.value, request.artifactId.value, request.launchProfileId.map(_.value), request.lifecycleAction.value, request.operatorSubjectId.value, request.deadlineAt)
        for {
          supervisor <- standalone_supervisor(request.artifactId.value, request.launchProfileId.map(_.value))
          result = supervisor.fold(
            code => LifecycleSupervisorProtocol.unavailable(protocolrequest, request.supervisorId.map(_.value).getOrElse(""), code, now),
            value => value.lookup(protocolrequest.requestId).getOrElse(value.submit(protocolrequest, now))
          )
          patch <- exec_from(lifecycle_request_update(result))
          _ <- entity_update(request.id, patch)
        } yield lifecycle_request_entity(request, result)
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
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      core.component.flatMap(_.eventReception) match {
        case Some(reception) =>
          reception.receiveInternal(ReceptionInput(
            name = routableevent.name,
            kind = routableevent.kind,
            payload = routableevent.payload,
            attributes = routableevent.attributes,
            persistent = false
          )).flatMap { result =>
            if (result.outcome == ReceptionOutcome.Routed && result.dispatchedCount > 0) Consequence.unit
            else Consequence.stateConflict(s"Lifecycle request event was not routed: ${routableevent.name}: $result")
          }
        case None => Consequence.stateConflict(s"Lifecycle request event reception is not initialized: ${routableevent.name}")
      }
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

final class EntityServiceFactoryImpl extends TextusControlCenterComponent.EntityServiceFactory

object EntityServiceFactoryImpl {
  def apply(): EntityServiceFactoryImpl = new EntityServiceFactoryImpl()
}

final class AggregateServiceFactoryImpl extends TextusControlCenterComponent.AggregateServiceFactory

object AggregateServiceFactoryImpl {
  def apply(): AggregateServiceFactoryImpl = new AggregateServiceFactoryImpl()
}

final class ViewServiceFactoryImpl extends TextusControlCenterComponent.ViewServiceFactory

object ViewServiceFactoryImpl {
  def apply(): ViewServiceFactoryImpl = new ViewServiceFactoryImpl()
}
