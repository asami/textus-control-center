/*
 * @version Jul. 19, 2026
 */
package org.simplemodeling.textus.controlcenter.impl

import java.time.{Duration, Instant}

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityQuery, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.context.SecurityContext
import org.goldenport.cncf.security.AuthenticationProvider
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.TextusControlCenterComponent
import org.simplemodeling.textus.controlcenter.entity.{RegisteredSubsystem as RegisteredSubsystemEntity}
import org.simplemodeling.textus.controlcenter.entity.{ManagedCar as ManagedCarEntity, ManagedCarSource as ManagedCarSourceEntity}
import org.simplemodeling.textus.controlcenter.entity.create.{RegisteredSubsystem as RegisteredSubsystemCreate}
import org.simplemodeling.textus.controlcenter.entity.create.RegisteredSubsystem.given
import org.simplemodeling.textus.controlcenter.entity.query.{RegisteredSubsystem as RegisteredSubsystemQuery}
import org.simplemodeling.textus.controlcenter.entity.query.{ManagedCar as ManagedCarQuery, ManagedCarSource as ManagedCarSourceQuery}
import org.simplemodeling.textus.controlcenter.entity.create.{ManagedCar as ManagedCarCreate, ManagedCarSource as ManagedCarSourceCreate}
import org.simplemodeling.textus.controlcenter.entity.create.ManagedCar.given
import org.simplemodeling.textus.controlcenter.entity.create.ManagedCarSource.given
import org.simplemodeling.textus.controlcenter.catalog.{DevelopmentRoot, LocalRepositoryCatalog, ManagedCarSource as CatalogManagedCarSource, PublicRepositoryCatalog, StandaloneDevelopmentCatalogProvider, StandaloneLocalRepositoryCatalogProvider, StandalonePublicRepositoryCatalogProvider}
import org.simplemodeling.textus.controlcenter.registry.{RegisteredSubsystem as RegistrySubsystem, RegistryError, RegistrationInput, SubsystemRegistry}

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

    protected final def safe_projection(source: RegistrySubsystem, now: Instant): Record =
      SubsystemRegistry.projection(source, now, STALE_THRESHOLD) match {
        case Right(projection) => Record.dataAuto(
          "protocolVersion" -> projection.protocolVersion,
          "instanceId" -> projection.instanceId,
          "launcherKind" -> projection.launcherKind,
          "target" -> projection.target,
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
        root = config_string("textus-control-center.catalog.development.root").map(_.trim).filter(_.nonEmpty)
        localcatalog = config_string("textus-control-center.catalog.local-repository.catalog-root").map(_.trim).filter(_.nonEmpty)
        publicbase = config_string("textus-control-center.catalog.public.base-url").map(_.trim).filter(_.nonEmpty)
        publicsubscriptions = config_string("textus-control-center.catalog.public.artifact-ids").toVector.flatMap(_.split(',').toVector.map(_.trim).filter(_.nonEmpty)).distinct
        publicrepository = publicbase.filter(_ => publicsubscriptions.nonEmpty).map(base => PublicRepositoryCatalog("simplemodeling-public", base, publicsubscriptions))
        existingcars <- find_managed_cars_all
        existingsources <- find_managed_sources_all
        local = root.toVector.flatMap(path => StandaloneDevelopmentCatalogProvider.discover(DevelopmentRoot("standalone-development", path), now)) ++ localcatalog.toVector.flatMap(path => StandaloneLocalRepositoryCatalogProvider.discover(LocalRepositoryCatalog("standalone-local-repository", path), now))
        public <- publicrepository.toVector.traverse(fetch_public_sources(_, now)).map(_.flatten)
        discovered = local ++ public
        stored <- discovered.traverse(persist_discovered_source(_, now, existingcars, existingsources))
      } yield OperationResponse(Record.dataAuto(
        "artifactId" -> action.record.getString("artifactId").map(_.trim).filter(_.nonEmpty),
        "refreshedAt" -> now,
        "refreshedSourceCount" -> stored.size,
        "adapterState" -> (if (root.isDefined || localcatalog.isDefined || publicrepository.isDefined) "configured" else "not-configured")
      ))
  }

  private final case class ListManagedCarsActionCallImpl(core: ActionCall.Core, override val action: ListManagedCars)
      extends ListManagedCarsActionCall with CatalogActionSupport {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(administrative_principal)
        cars <- find_managed_cars_all
        sources <- find_managed_sources_all
        text = action.record.getString("text").map(_.trim.toLowerCase).filter(_.nonEmpty)
        offset = action.record.getInt("offset").getOrElse(0).max(0)
        limit = action.record.getInt("limit").getOrElse(100).max(0)
        filtered = latest_cars(cars).filter(car => text.forall(value => matches_text(car, value)))
        page = filtered.drop(offset).take(limit)
      } yield OperationResponse(Record.dataAuto(
        "data" -> page.map(car => safe_car_projection(car, latest_sources(sources).filter(_.artifactId == car.artifactId), false)),
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
      } yield OperationResponse(safe_car_projection(car, latest_sources(sources).filter(_.artifactId == artifactid), true))
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
    protected final def fetch_public_sources(repository: PublicRepositoryCatalog, now: Instant): ExecUowM[Vector[CatalogManagedCarSource]] =
      repository.subscriptions.traverse { artifactid =>
        http_get(StandalonePublicRepositoryCatalogProvider.catalog_url(repository, artifactid), Map("Accept" -> "application/yaml, text/yaml"))
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
        retained = retain_source_facts(source, existingsources)
        recommended = retained.availableVersions.headOption
        latest = retained.availableVersions.lastOption
        stored <- entity_create(ManagedCarSourceCreate(None, retained.artifactId, retained.sourceId, retained.sourceKind.mark, retained.refreshState.toString.toLowerCase, retained.componentName, recommended, latest, retained.snapshotAt, retained.diagnostic, retained.privateLocator))
      } yield ManagedCarSourceEntity(stored.id, retained.artifactId, retained.sourceId, retained.sourceKind.mark, retained.refreshState.toString.toLowerCase, retained.componentName, recommended, latest, retained.snapshotAt, retained.diagnostic, retained.privateLocator)
    protected final def ensure_managed_car(source: CatalogManagedCarSource, existing: Vector[ManagedCarEntity], now: Instant): ExecUowM[Unit] =
      existing.find(_.artifactId == source.artifactId) match {
        case Some(car) => entity_update(car.copy(componentName = source.componentName.orElse(car.componentName), lastObservedAt = now)).map(_ => ())
        case None => entity_create(ManagedCarCreate(None, source.artifactId, source.componentName, now, now)).map(_ => ())
      }
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
    protected final def safe_car_projection(car: ManagedCarEntity, sources: Vector[ManagedCarSourceEntity], detail: Boolean): Record =
      Record.dataAuto("artifactId" -> car.artifactId, "componentName" -> car.componentName, "createdAt" -> car.firstObservedAt, "updatedAt" -> car.lastObservedAt, "sources" -> sources.map(source => Record.dataAuto("sourceId" -> source.sourceId, "sourceKind" -> source.sourceKind, "refreshState" -> source.refreshState, "componentName" -> source.componentName, "recommendedVersion" -> source.recommendedVersion, "latestVersion" -> source.latestVersion, "snapshotAt" -> source.snapshotAt, "diagnostic" -> source.diagnostic, "privateLocator" -> (if (detail) source.privateLocator else None))))
    protected final def matches_text(car: ManagedCarEntity, text: String): Boolean = Vector(car.artifactId).concat(car.componentName.toVector).exists(_.toLowerCase.contains(text))
  }
}

object SubsystemInventoryServiceFactoryImpl {
  def apply(): SubsystemInventoryServiceFactoryImpl = new SubsystemInventoryServiceFactoryImpl()
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
