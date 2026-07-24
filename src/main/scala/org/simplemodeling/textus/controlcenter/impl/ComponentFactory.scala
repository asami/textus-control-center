/*
 * @version Jul. 24, 2026
 */
package org.simplemodeling.textus.controlcenter.impl

import java.time.{Duration, Instant}
import java.util.UUID

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityQuery, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.context.SecurityContext
import org.goldenport.cncf.event.{CmlEventCategory, CmlEventDefinition, CmlSubscriptionDefinition, DispatchRoute, EventOriginBoundary, EventReceptionCondition, EventReceptionExecutionPolicy, EventReceptionRule, ReceptionDomainEvent, ReceptionInput, ReceptionOutcome}
import org.goldenport.cncf.security.AuthenticationProvider
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.TextusControlCenterComponent
import org.simplemodeling.textus.controlcenter.entity.{RegisteredSubsystem as RegisteredSubsystemEntity}
import org.simplemodeling.textus.controlcenter.entity.{ManagedCar as ManagedCarEntity, ManagedCarSource as ManagedCarSourceEntity}
import org.simplemodeling.textus.controlcenter.entity.{OperationalComponent as OperationalComponentEntity}
import org.simplemodeling.textus.controlcenter.entity.{LifecycleRequest as LifecycleRequestEntity}
import org.simplemodeling.textus.controlcenter.entity.{LauncherEvidenceRecord as LauncherEvidenceRecordEntity}
import org.simplemodeling.textus.controlcenter.entity.create.{RegisteredSubsystem as RegisteredSubsystemCreate}
import org.simplemodeling.textus.controlcenter.entity.create.RegisteredSubsystem.given
import org.simplemodeling.textus.controlcenter.entity.query.{RegisteredSubsystem as RegisteredSubsystemQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{ManagedCar as ManagedCarQuery, ManagedCarSource as ManagedCarSourceQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{OperationalComponent as OperationalComponentQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{LifecycleRequest as LifecycleRequestQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{LauncherEvidenceRecord as LauncherEvidenceRecordQuery}
import org.simplemodeling.textus.controlcenter.entity.create.{ManagedCar as ManagedCarCreate, ManagedCarSource as ManagedCarSourceCreate}
import org.simplemodeling.textus.controlcenter.entity.create.{OperationalComponent as OperationalComponentCreate}
import org.simplemodeling.textus.controlcenter.entity.create.{LifecycleRequest as LifecycleRequestCreate}
import org.simplemodeling.textus.controlcenter.entity.create.{LauncherEvidenceRecord as LauncherEvidenceRecordCreate}
import org.simplemodeling.textus.controlcenter.entity.update.{LauncherEvidenceRecord as LauncherEvidenceRecordUpdate}
import org.simplemodeling.textus.controlcenter.entity.update.{LifecycleRequest as LifecycleRequestUpdate, ManagedCar as ManagedCarUpdate, OperationalComponent as OperationalComponentUpdate}
import org.simplemodeling.textus.controlcenter.entity.create.ManagedCar.given
import org.simplemodeling.textus.controlcenter.entity.create.ManagedCarSource.given
import org.simplemodeling.textus.controlcenter.entity.create.OperationalComponent.given
import org.simplemodeling.textus.controlcenter.entity.create.LifecycleRequest.given
import org.simplemodeling.textus.controlcenter.entity.create.LauncherEvidenceRecord.given
import org.simplemodeling.textus.controlcenter.catalog.{DevelopmentRoot, LocalRepositoryCatalog, ManagedCar as CatalogManagedCar, ManagedCarCatalog, ManagedCarSource as CatalogManagedCarSource, OperationalComponentManagement, OperationalManagementState, PublicRepositoryCatalog, RuntimeInstance, RuntimeInstanceStatus, StandaloneCatalogConfiguration, StandaloneDevelopmentCatalogProvider, StandaloneLocalRepositoryCatalogProvider, StandalonePublicRepositoryCatalogProvider}
import org.simplemodeling.textus.controlcenter.registry.{RegisteredSubsystem as RegistrySubsystem, RegistryError, RegistrationInput, SubsystemRegistry}
import org.simplemodeling.textus.controlcenter.supervisor.{LifecycleSupervisorProtocol, LifecycleSupervisorRequest, LifecycleSupervisorResult}
import org.simplemodeling.textus.controlcenter.launcher.{LauncherEvidenceClient, LauncherEvidenceClientConfiguration, LauncherEvidenceEntry, LauncherLifecycleClient, LauncherLifecycleClientConfiguration}

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
) extends TextusControlCenterComponent {
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
      TextusControlCenterLauncherRegistrationAuthenticationProvider.fromConfiguration(params.subsystem.configuration)
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
        _ <- ensure_adopted_operational_component(stored.artifactId, now)
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
      } yield result.data.filter(_.instanceId == instanceid)

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
        source.instanceId,
        source.launcherKind,
        source.target,
        source.artifactId,
        source.executionMode,
        source.developmentDirectory,
        source.subsystemName,
        source.subsystemVersion,
        source.runtimeVersion,
        source.baseUrl,
        source.hostLabel,
        source.startedAt,
        source.lastSeenAt,
        source.launcherState,
        source.registrationPrincipalId
      )

    protected final def to_create(
      source: RegistrySubsystem
    ): RegisteredSubsystemCreate =
      RegisteredSubsystemCreate(
        None,
        source.instanceId,
        source.protocolVersion,
        source.launcherKind,
        source.target,
        source.artifactId,
        source.executionMode,
        source.developmentDirectory,
        source.subsystemName,
        source.subsystemVersion,
        source.runtimeVersion,
        source.baseUrl,
        source.hostLabel,
        source.startedAt,
        source.lastSeenAt,
        source.launcherState,
        source.registrationPrincipalId
      )

    protected final def persist(source: RegistrySubsystem): ExecUowM[RegisteredSubsystemEntity] =
      entity_create(to_create(source)).map(result => RegisteredSubsystemEntity(
        result.id,
        source.instanceId,
        source.protocolVersion,
        source.launcherKind,
        source.target,
        source.artifactId,
        source.executionMode,
        source.developmentDirectory,
        source.subsystemName,
        source.subsystemVersion,
        source.runtimeVersion,
        source.baseUrl,
        source.hostLabel,
        source.startedAt,
        source.lastSeenAt,
        source.launcherState,
        source.registrationPrincipalId
      ))

    protected final def ensure_adopted_operational_component(artifactid: Option[String], now: Instant): ExecUowM[Unit] =
      artifactid match {
        case Some(value) =>
          for {
            existing <- find_operational_components(value)
            _ <- latest_operational_component(existing) match {
              case Some(_) => exec_from(Consequence.unit)
              case None => entity_create(OperationalComponentCreate(None, value, "adopted", now, now)).map(_ => ())
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
      } yield result.data.filter(_.artifactId == artifactid)

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
        public <- publicrepositories.traverse(fetch_public_sources(_, now, refreshtimeout)).map(_.flatten)
        discovered = local ++ public
        stored <- discovered.traverse(persist_discovered_source(_, now, existingcars, existingsources))
      } yield OperationResponse(Record.dataAuto(
        "artifactId" -> action.record.getString("artifactId").map(_.trim).filter(_.nonEmpty),
        "refreshedAt" -> now,
        "refreshedSourceCount" -> stored.size,
        "adapterState" -> (if (catalogfile.isDefined && catalogconfiguration.isEmpty) "invalid" else if (developmentroots.nonEmpty || localcatalogs.nonEmpty || publicrepositories.nonEmpty) "configured" else "not-configured")
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
        car <- exec_from(latest_cars(cars).find(_.artifactId == artifactid).toRight(RegistryError.Missing(artifactid)).fold(registry_error, Consequence.success))
        sources <- find_managed_sources_all
        registered <- find_registered_subsystems_all
        now = core.executionContext.clock.instant()
      } yield OperationResponse(safe_car_projection(car, latest_sources(sources).filter(_.artifactId == artifactid), true, runtime_summary(car, latest_cars(cars), registered, now)))
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
    protected final def fetch_public_sources(repository: PublicRepositoryCatalog, now: Instant, timeout: Duration): ExecUowM[Vector[CatalogManagedCarSource]] =
      repository.subscriptions.traverse { artifactid =>
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
        stored <- entity_create(ManagedCarSourceCreate(None, retained.artifactId, retained.sourceId, retained.sourceKind.mark, retained.refreshState.toString.toLowerCase, retained.componentName, recommended, latest, retained.snapshotAt, retained.diagnostic, retained.privateLocator))
      } yield ManagedCarSourceEntity(stored.id, retained.artifactId, retained.sourceId, retained.sourceKind.mark, retained.refreshState.toString.toLowerCase, retained.componentName, recommended, latest, retained.snapshotAt, retained.diagnostic, retained.privateLocator)
    protected final def ensure_managed_car(source: CatalogManagedCarSource, existing: Vector[ManagedCarEntity], now: Instant): ExecUowM[Unit] =
      existing.find(_.artifactId == source.artifactId) match {
        case Some(car) =>
          for {
            patch <- exec_from(managed_car_update(source.componentName.orElse(car.componentName), now))
            _ <- entity_update(car.id, patch)
          } yield ()
        case None => entity_create(ManagedCarCreate(None, source.artifactId, source.componentName, now, now)).map(_ => ())
      }
    protected final def ensure_auto_managed_operational_component(source: CatalogManagedCarSource, now: Instant): ExecUowM[Unit] =
      if (source.sourceKind == org.simplemodeling.textus.controlcenter.catalog.ManagedCarSourceKind.Development) {
        for {
          existing <- find_operational_components(source.artifactId)
          _ <- latest_operational_component(existing) match {
            case Some(component) if component.managementState == "excluded" => exec_from(Consequence.unit)
            case Some(component) =>
              for {
                patch <- exec_from(operational_component_update("auto-managed", now))
                _ <- entity_update(component.id, patch)
              } yield ()
            case None => entity_create(OperationalComponentCreate(None, source.artifactId, "auto-managed", now, now)).map(_ => ())
          }
        } yield ()
      } else {
        exec_from(Consequence.unit)
      }
    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId == artifactid)
    protected final def latest_operational_component(sources: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      sources.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption
    protected final def managed_car_update(componentname: Option[String], now: Instant): Consequence[ManagedCarUpdate] =
      componentname match {
        case Some(value) => new ManagedCarUpdate.Builder().withComponentName(value).withLastObservedAt(now).buildC()
        case None => new ManagedCarUpdate.Builder().withLastObservedAt(now).buildC()
      }
    protected final def operational_component_update(managementstate: String, now: Instant): Consequence[OperationalComponentUpdate] =
      new OperationalComponentUpdate.Builder().withManagementState(managementstate).withLastObservedAt(now).buildC()
    protected final def retain_source_facts(source: CatalogManagedCarSource, existing: Vector[ManagedCarSourceEntity]): CatalogManagedCarSource =
      if (source.refreshState == org.simplemodeling.textus.controlcenter.catalog.ManagedCarRefreshState.Available) source
      else latest_sources(existing).find(current => current.artifactId == source.artifactId && current.sourceId == source.sourceId) match {
        case Some(current) => source.copy(
          componentName = source.componentName.orElse(current.componentName),
          availableVersions = if (source.availableVersions.nonEmpty) source.availableVersions else Vector(current.recommendedVersion, current.latestVersion).flatten.distinct
        )
        case None => source
      }
    protected final def latest_cars(sources: Vector[ManagedCarEntity]): Vector[ManagedCarEntity] =
      sources.groupBy(_.artifactId).valuesIterator.flatMap(_.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption).toVector.sortBy(_.artifactId)
    protected final def latest_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] =
      sources.groupBy(source => (source.artifactId, source.sourceId)).valuesIterator.flatMap(_.sortBy(source => (source.snapshotAt, source.id.print)).lastOption).toVector.sortBy(source => (source.artifactId, source.sourceKind, source.sourceId))
    protected final def latest_registered_subsystems(sources: Vector[RegisteredSubsystemEntity]): Vector[RegisteredSubsystemEntity] =
      sources.groupBy(_.instanceId).valuesIterator.flatMap(_.sortBy(source => (source.lastSeenAt, source.id.print)).lastOption).toVector.sortBy(_.instanceId)
    protected final def runtime_summary(car: ManagedCarEntity, cars: Vector[ManagedCarEntity], registered: Vector[RegisteredSubsystemEntity], now: Instant) = {
      val catalogcars = cars.map(source => CatalogManagedCar(source.artifactId, source.componentName, source.componentName.toSet, Vector.empty, Vector.empty))
      val instances = latest_registered_subsystems(registered).map { source =>
        val registry = RegistrySubsystem(source.protocolVersion, source.instanceId, source.launcherKind, source.target, source.artifactId, source.executionMode, source.developmentDirectory, source.subsystemName, source.subsystemVersion, source.runtimeVersion, source.baseUrl, source.hostLabel, source.startedAt, source.lastSeenAt, source.launcherState, source.registrationPrincipalId)
        val status = SubsystemRegistry.projection(registry, now, Duration.ofSeconds(90)).toOption.map(_.status) match {
          case Some(SubsystemRegistry.running) => RuntimeInstanceStatus.Running
          case Some(SubsystemRegistry.starting) => RuntimeInstanceStatus.Starting
          case Some(SubsystemRegistry.stale) => RuntimeInstanceStatus.Stale
          case _ => RuntimeInstanceStatus.Stopped
        }
        RuntimeInstance(source.instanceId, source.artifactId, source.target, source.subsystemName, status)
      }
      ManagedCarCatalog.runtimeSummary(car.artifactId, instances, ManagedCarCatalog.linkRuntimeInstances(catalogcars, instances))
    }
    protected final def safe_car_projection(car: ManagedCarEntity, sources: Vector[ManagedCarSourceEntity], detail: Boolean, runtime: org.simplemodeling.textus.controlcenter.catalog.ManagedCarRuntimeSummary): Record =
      Record.dataAuto("artifactId" -> car.artifactId, "componentName" -> car.componentName, "createdAt" -> car.firstObservedAt, "updatedAt" -> car.lastObservedAt, "runtimeState" -> (runtime.state match { case org.simplemodeling.textus.controlcenter.catalog.ManagedCarRuntimeState.NotRunning => "not-running"; case state => state.toString.toLowerCase }), "activeInstanceIds" -> runtime.activeInstanceIds, "staleInstanceIds" -> runtime.staleInstanceIds, "sources" -> sources.map(source => Record.dataAuto("sourceId" -> source.sourceId, "sourceKind" -> source.sourceKind, "refreshState" -> source.refreshState, "componentName" -> source.componentName, "recommendedVersion" -> source.recommendedVersion, "latestVersion" -> source.latestVersion, "snapshotAt" -> source.snapshotAt, "diagnostic" -> source.diagnostic, "privateLocator" -> (if (detail) source.privateLocator else None))))
    protected final def matches_text(car: ManagedCarEntity, text: String): Boolean = Vector(car.artifactId).concat(car.componentName.toVector).exists(_.toLowerCase.contains(text))
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
        filtered = latest_operational_components(components).filter(component => component.managementState != "excluded" && text.forall(value => component.artifactId.toLowerCase.contains(value)))
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
        stored = component.copy(managementState = "excluded", lastObservedAt = now)
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
        developmentavailable = latest_managed_sources(sources).exists(source => source.artifactId == artifactid && source.sourceKind == "DEV" && source.refreshState == "available")
        accepteduse = registered.exists(_.artifactId.contains(artifactid))
        state <- exec_from(OperationalComponentManagement.reconcile(Some(OperationalManagementState.Excluded), developmentavailable, accepteduse, exclusionrecordexists = false).toRight("No development source or accepted launcher use evidence exists.").fold(Consequence.operationInvalid, Consequence.success))
        now = core.executionContext.clock.instant()
        patch <- exec_from(operational_component_update(state.mark, now))
        _ <- entity_update(component.id, patch)
        stored = component.copy(managementState = state.mark, lastObservedAt = now)
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
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId == artifactid)
    protected final def find_operational_components_all: ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data
    protected final def find_managed_sources_all: ExecUowM[Vector[ManagedCarSourceEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "ManagedCarSource")); query = EntityQuery[ManagedCarSourceEntity](ManagedCarSourceQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[ManagedCarSourceEntity](query) } yield result.data
    protected final def find_registered_subsystems_all: ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem")); query = EntityQuery[RegisteredSubsystemEntity](RegisteredSubsystemQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[RegisteredSubsystemEntity](query) } yield result.data
    protected final def latest_operational_component(sources: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] = sources.sortBy(source => (source.lastObservedAt, source.id.print)).lastOption
    protected final def latest_operational_components(sources: Vector[OperationalComponentEntity]): Vector[OperationalComponentEntity] = sources.groupBy(_.artifactId).valuesIterator.flatMap(latest_operational_component).toVector.sortBy(_.artifactId)
    protected final def latest_managed_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] = sources.groupBy(source => (source.artifactId, source.sourceId)).valuesIterator.flatMap(source => source.sortBy(value => (value.snapshotAt, value.id.print)).lastOption).toVector
    protected final def operational_component_update(managementstate: String, now: Instant): Consequence[OperationalComponentUpdate] =
      new OperationalComponentUpdate.Builder().withManagementState(managementstate).withLastObservedAt(now).buildC()
    protected final def safe_projection(component: OperationalComponentEntity): Record = Record.dataAuto("artifactId" -> component.artifactId, "managementState" -> component.managementState, "firstManagedAt" -> component.firstManagedAt, "lastObservedAt" -> component.lastObservedAt)
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
        records <- projection.entries.traverse(entry => retain(entry, existing, registered.filterNot(_.launcherState == "stopped").map(_.instanceId).toSet, now))
      } yield OperationResponse(Record.dataAuto("data" -> records.map(safe_projection), "totalCount" -> records.size, "observedAt" -> now))
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
        records = latest(values).filter(record => text.forall(value => Vector(record.instanceId, record.launcherKind, record.target).exists(_.toLowerCase.contains(value))))
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
        record <- exec_from(latest(values).find(_.instanceId == instanceid).toRight(instanceid).fold(Consequence.resourceNotFound, Consequence.success))
        detail = launcher_evidence_client.toOption.flatMap(_.detail(instanceid).toOption)
      } yield OperationResponse(Record.dataAuto(
        "instanceId" -> record.instanceId,
        "launcherKind" -> record.launcherKind,
        "target" -> record.target,
        "artifactId" -> record.artifactId,
        "executionMode" -> record.executionMode,
        "subsystemName" -> record.subsystemName,
        "subsystemVersion" -> record.subsystemVersion,
        "runtimeVersion" -> record.runtimeVersion,
        "startedAt" -> record.startedAt,
        "lastSeenAt" -> record.lastSeenAt,
        "stoppedAt" -> record.stoppedAt,
        "evidenceDecision" -> record.evidenceDecision,
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
    protected final def find_all: ExecUowM[Vector[LauncherEvidenceRecordEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "LauncherEvidenceRecord")); query = EntityQuery[LauncherEvidenceRecordEntity](LauncherEvidenceRecordQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[LauncherEvidenceRecordEntity](query) } yield result.data
    protected final def find_registered_subsystems_all: ExecUowM[Vector[RegisteredSubsystemEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "RegisteredSubsystem")); query = EntityQuery[RegisteredSubsystemEntity](RegisteredSubsystemQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[RegisteredSubsystemEntity](query) } yield result.data
    protected final def find_operational_components(artifactid: String): ExecUowM[Vector[OperationalComponentEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId == artifactid)
    protected final def latest(values: Vector[LauncherEvidenceRecordEntity]): Vector[LauncherEvidenceRecordEntity] =
      values.groupBy(_.instanceId).valuesIterator.flatMap(_.sortBy(value => (value.observedAt, value.id.print)).lastOption).toVector.sortBy(value => (value.target, value.instanceId))
    protected final def latest_operational_component(values: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      values.sortBy(value => (value.lastObservedAt, value.id.print)).lastOption
    protected final def retain(entry: LauncherEvidenceEntry, existing: Vector[LauncherEvidenceRecordEntity], registeredids: Set[String], now: Instant): ExecUowM[LauncherEvidenceRecordEntity] = {
      val decision = if (entry.stoppedAt.isDefined) "historical-stopped" else if (registeredids.contains(entry.instanceId)) "current-registered" else "current-evidence-only"
      for {
        record <- latest(existing).find(_.instanceId == entry.instanceId) match {
          case Some(current) =>
            for {
              patch <- exec_from(new LauncherEvidenceRecordUpdate.Builder().withLauncherKind(entry.launcherKind).withTarget(entry.target).withArtifactId(entry.artifactId.fold(org.simplemodeling.model.directive.Update.setNull[String])(org.simplemodeling.model.directive.Update.set)).withExecutionMode(entry.executionMode).withSubsystemName(entry.subsystemName.fold(org.simplemodeling.model.directive.Update.setNull[String])(org.simplemodeling.model.directive.Update.set)).withSubsystemVersion(entry.subsystemVersion.fold(org.simplemodeling.model.directive.Update.setNull[String])(org.simplemodeling.model.directive.Update.set)).withRuntimeVersion(entry.runtimeVersion).withStartedAt(entry.startedAt).withLastSeenAt(entry.lastSeenAt).withStoppedAt(entry.stoppedAt.fold(org.simplemodeling.model.directive.Update.setNull[Instant])(org.simplemodeling.model.directive.Update.set)).withEvidenceDecision(decision).withObservedAt(now).buildC())
              _ <- entity_update(current.id, patch)
            } yield current.copy(launcherKind = entry.launcherKind, target = entry.target, artifactId = entry.artifactId, executionMode = entry.executionMode, subsystemName = entry.subsystemName, subsystemVersion = entry.subsystemVersion, runtimeVersion = entry.runtimeVersion, startedAt = entry.startedAt, lastSeenAt = entry.lastSeenAt, stoppedAt = entry.stoppedAt, evidenceDecision = decision, observedAt = now)
          case None =>
            entity_create(LauncherEvidenceRecordCreate(None, entry.instanceId, entry.launcherKind, entry.target, entry.artifactId, entry.executionMode, None, entry.subsystemName, entry.subsystemVersion, entry.runtimeVersion, entry.startedAt, entry.lastSeenAt, entry.stoppedAt, decision, now)).map { stored =>
              LauncherEvidenceRecordEntity(stored.id, entry.instanceId, entry.launcherKind, entry.target, entry.artifactId, entry.executionMode, None, entry.subsystemName, entry.subsystemVersion, entry.runtimeVersion, entry.startedAt, entry.lastSeenAt, entry.stoppedAt, decision, now)
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
          case None => entity_create(OperationalComponentCreate(None, artifactid, "adopted", now, now)).map(_ => ())
        }
      } yield ()
    protected final def safe_projection(record: LauncherEvidenceRecordEntity): Record = Record.dataAuto(
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
        request <- exec_from(requests.find(_.requestId == requestid).toRight(requestid).fold(Consequence.resourceNotFound, Consequence.success))
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
        request <- exec_from(requests.find(_.requestId == requestid).toRight(requestid).fold(Consequence.resourceNotFound, Consequence.success))
        reconciled <- if (request.requestState == "queued") reconcile_lifecycle_request(request) else exec_pure(request)
      } yield OperationResponse(safe_projection(reconciled))
  }

  private trait LifecycleControlActionSupport { self: ActionCall =>
    protected final def lifecycle_request(actionname: String, record: Record): ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        artifactid <- exec_from(required_string(record, "artifactId"))
        idempotencykey <- exec_from(required_string(record, "idempotencyKey"))
        requests <- find_lifecycle_requests(artifactid)
        response <- requests.find(value => value.lifecycleAction == actionname && value.idempotencyKey == idempotencykey) match {
          case Some(existing) => exec_pure(OperationResponse(safe_projection(existing)))
          case None => _create_lifecycle_request(artifactid, actionname, idempotencykey)
        }
      } yield response

    private def _create_lifecycle_request(artifactid: String, actionname: String, idempotencykey: String): ExecUowM[OperationResponse] =
      for {
        components <- find_operational_components(artifactid)
        component <- exec_from(latest_operational_component(components).toRight(artifactid).fold(Consequence.resourceNotFound, Consequence.success))
        now = core.executionContext.clock.instant()
        launcherconfiguration = launcher_lifecycle_configuration
        deadlineat = now.plus(launcherconfiguration.toOption.map(_.timeout).getOrElse(Duration.ofSeconds(5)))
        queued = component.managementState != "excluded" && launcherconfiguration.isRight
        state = if (queued) "queued" else "rejected"
        diagnostic = if (queued) None else Some(lifecycle_diagnostic(component, launcherconfiguration))
        completedat = if (queued) None else Some(now)
        requestid = UUID.randomUUID().toString
        stored <- entity_create(LifecycleRequestCreate(
          None,
          requestid,
          artifactid,
          actionname,
          state,
          idempotencykey,
          now,
          deadlineat,
          None,
          completedat,
          None,
          diagnostic,
          diagnostic,
          executionContext.security.principal.id.value,
          None,
          None
        ))
        request = LifecycleRequestEntity(
          stored.id,
          requestid,
          artifactid,
          actionname,
          state,
          idempotencykey,
          now,
          deadlineat,
          None,
          completedat,
          None,
          diagnostic,
          diagnostic,
          executionContext.security.principal.id.value,
          None,
          None
        )
        _ <- if (queued) exec_from(stage_lifecycle_dispatch_event(request.requestId)) else exec_pure(())
      } yield OperationResponse(safe_projection(request))

    protected final def dispatch_lifecycle_request(request: LifecycleRequestEntity): ExecUowM[LifecycleRequestEntity] =
      if (request.requestState != "queued") exec_pure(request)
      else {
        val now = core.executionContext.clock.instant()
        val launcherconfiguration = launcher_lifecycle_configuration
        val protocolrequest = LifecycleSupervisorRequest(
          request.requestId,
          request.idempotencyKey,
          request.artifactId,
          request.lifecycleAction,
          request.operatorSubjectId,
          request.deadlineAt
        )
        val result = launcherconfiguration match {
          case Right(configuration) => submit_lifecycle_request(configuration, protocolrequest, now)
          case Left(code) => LifecycleSupervisorProtocol.unavailable(protocolrequest, request.supervisorId.getOrElse(""), code, now)
        }
        for {
          patch <- exec_from(lifecycle_request_update(result))
          _ <- entity_update(request.id, patch)
        } yield lifecycle_request_entity(request, result)
      }

    protected final def reconcile_lifecycle_request(request: LifecycleRequestEntity): ExecUowM[LifecycleRequestEntity] =
      if (request.requestState != "queued") exec_pure(request)
      else {
        val now = core.executionContext.clock.instant()
        val launcherconfiguration = launcher_lifecycle_configuration
        val protocolrequest = LifecycleSupervisorRequest(request.requestId, request.idempotencyKey, request.artifactId, request.lifecycleAction, request.operatorSubjectId, request.deadlineAt)
        val result = launcherconfiguration match {
          case Right(configuration) => lookup_lifecycle_request(configuration, protocolrequest).getOrElse(submit_lifecycle_request(configuration, protocolrequest, now))
          case Left(code) => LifecycleSupervisorProtocol.unavailable(protocolrequest, request.supervisorId.getOrElse(""), code, now)
        }
        for { patch <- exec_from(lifecycle_request_update(result)); _ <- entity_update(request.id, patch) } yield lifecycle_request_entity(request, result)
      }

    protected final def submit_lifecycle_request(
      configuration: LauncherLifecycleClientConfiguration,
      request: LifecycleSupervisorRequest,
      now: Instant
    ): LifecycleSupervisorResult =
      LauncherLifecycleClient(configuration).submit(request).getOrElse(
        LifecycleSupervisorProtocol.unavailable(request, "", "launcher-lifecycle-unavailable", now)
      )

    protected final def lookup_lifecycle_request(configuration: LauncherLifecycleClientConfiguration, request: LifecycleSupervisorRequest): Option[LifecycleSupervisorResult] =
      LauncherLifecycleClient(configuration).lookup(request.requestId)

    protected final def lifecycle_request_update(result: LifecycleSupervisorResult): Consequence[LifecycleRequestUpdate] =
      new LifecycleRequestUpdate.Builder()
        .withRequestState(result.state)
        .withAcceptedAt(result.acceptedAt.orNull)
        .withCompletedAt(result.completedAt.orNull)
        .withDiagnosticCode(result.diagnosticCode.orNull)
        .withDiagnostic(result.diagnostic.orNull)
        .withSupervisorId(result.supervisorId)
        .withInstanceId(result.instanceId.orNull)
        .buildC()

    protected final def lifecycle_request_entity(request: LifecycleRequestEntity, result: LifecycleSupervisorResult): LifecycleRequestEntity =
      request.copy(
        requestState = result.state,
        acceptedAt = result.acceptedAt,
        completedAt = result.completedAt,
        diagnosticCode = result.diagnosticCode,
        diagnostic = result.diagnostic,
        supervisorId = Some(result.supervisorId).filter(_.nonEmpty),
        instanceId = result.instanceId
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
      core.executionContext.runtime.unitOfWork.stageEvent(routableevent)
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
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "OperationalComponent")); query = EntityQuery[OperationalComponentEntity](OperationalComponentQuery.collectionId, fields.rewrite(Query.fromRecord(Record.dataAuto("artifactId" -> artifactid))), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[OperationalComponentEntity](query) } yield result.data.filter(_.artifactId == artifactid)
    protected final def find_lifecycle_requests(artifactid: String): ExecUowM[Vector[LifecycleRequestEntity]] =
      for { values <- find_lifecycle_requests_all } yield values.filter(_.artifactId == artifactid)
    protected final def find_lifecycle_requests_all: ExecUowM[Vector[LifecycleRequestEntity]] =
      for { fields <- exec_pure(EntityQueryFieldResolver(core.component, "LifecycleRequest")); query = EntityQuery[LifecycleRequestEntity](LifecycleRequestQuery.collectionId, fields.rewrite(Query.fromRecord(Record.empty)), scope = EntitySearchScope.Store, visibilityScope = Some(EntityVisibilityScope.Admin)); result <- entity_search_internal[LifecycleRequestEntity](query) } yield result.data
    protected final def latest_operational_component(values: Vector[OperationalComponentEntity]): Option[OperationalComponentEntity] =
      values.sortBy(value => (value.lastObservedAt, value.id.print)).lastOption
    protected final def lifecycle_diagnostic(component: OperationalComponentEntity, launcherconfiguration: Either[String, LauncherLifecycleClientConfiguration]): String =
      if (component.managementState == "excluded") "component-not-managed"
      else launcherconfiguration.fold(identity, _ => "launcher-lifecycle-unavailable")
    protected final def launcher_lifecycle_configuration: Either[String, LauncherLifecycleClientConfiguration] =
      LauncherLifecycleClientConfiguration.fromProperties(
        Vector(
          LauncherLifecycleClientConfiguration.Command,
          LauncherLifecycleClientConfiguration.Timeout
        ).flatMap(key => config_string(key).map(key -> _)).toMap
      )
    protected final def safe_projection(request: LifecycleRequestEntity): Record = Record.dataAuto(
      "requestId" -> request.requestId,
      "artifactId" -> request.artifactId,
      "lifecycleAction" -> request.lifecycleAction,
      "requestState" -> request.requestState,
      "requestedAt" -> request.requestedAt,
      "deadlineAt" -> request.deadlineAt,
      "acceptedAt" -> request.acceptedAt,
      "completedAt" -> request.completedAt,
      "launchProfileId" -> request.launchProfileId,
      "diagnosticCode" -> request.diagnosticCode,
      "diagnostic" -> request.diagnostic,
      "supervisorId" -> request.supervisorId,
      "instanceId" -> request.instanceId
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
