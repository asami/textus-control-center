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
        filtered = operational_cars(cars).filter(car => text.forall(value => matches_text(car, value)))
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
    protected final def operational_cars(sources: Vector[ManagedCarEntity]): Vector[ManagedCarEntity] =
      latest_cars(sources).sortBy(car => (if (_is_control_center_artifact(car.artifactId.value)) 0 else 1, car.artifactId.value))
    protected final def latest_sources(sources: Vector[ManagedCarSourceEntity]): Vector[ManagedCarSourceEntity] =
      sources.groupBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value)).valuesIterator.flatMap(_.sortBy(source => (source.snapshotAt, source.id.print)).lastOption).toVector.sortBy(source => (source.artifactId.value, source.sourceKind.value, source.sourceId.value))
    protected final def latest_registered_subsystems(sources: Vector[RegisteredSubsystemEntity]): Vector[RegisteredSubsystemEntity] =
      sources.groupBy(_.instanceId.value).valuesIterator.flatMap(_.sortBy(source => (source.lastSeenAt, source.id.print)).lastOption).toVector.sortBy(_.instanceId.value)
    protected final def runtime_summary(car: ManagedCarEntity, cars: Vector[ManagedCarEntity], registered: Vector[RegisteredSubsystemEntity], now: Instant): ManagedCarRuntimeSummary = {
      if (_is_control_center_artifact(car.artifactId.value))
        ManagedCarRuntimeSummary(ManagedCarRuntimeState.Running, Vector.empty, Vector.empty)
      else {
      val catalogcars = cars.map(source => CatalogManagedCar(source.artifactId.value, source.componentName.map(_.value), source.componentName.map(_.value).toSet, Vector.empty, Vector.empty))
      val instances = latest_registered_subsystems(registered).map { source =>
        val registry = RegistrySubsystem(
          protocolVersion = source.protocolVersion,
          instanceId = source.instanceId.value,
          launcherKind = source.launcherKind.value,
          target = source.target.value,
          artifactId = source.artifactId.map(_.value),
          executionMode = source.executionMode.map(_.value),
          developmentDirectory = source.developmentDirectory.map(_.value),
          subsystemName = source.subsystemName.map(_.value),
          subsystemVersion = source.subsystemVersion.map(_.value),
          runtimeVersion = source.runtimeVersion.map(_.value),
          baseUrl = source.baseUrl.toExternalForm,
          hostLabel = source.hostLabel.toI18nString.displayMessage,
          startedAt = source.startedAt,
          lastSeenAt = source.lastSeenAt,
          launcherState = source.launcherState.value,
          registrationPrincipalId = source.registrationPrincipalId.value,
          applicationUrl = source.applicationUrl.map(_.toExternalForm)
        )
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
    }
    private def _is_control_center_artifact(artifactid: String): Boolean = artifactid == "textus-control-center"
    protected final def safe_car_projection(car: ManagedCarEntity, sources: Vector[ManagedCarSourceEntity], detail: Boolean, runtime: org.simplemodeling.textus.controlcenter.catalog.ManagedCarRuntimeSummary): Record =
      Record.dataAuto("artifactId" -> car.artifactId.value, "componentName" -> car.componentName.map(_.value), "createdAt" -> car.firstObservedAt, "updatedAt" -> car.lastObservedAt, "runtimeState" -> (runtime.state match { case org.simplemodeling.textus.controlcenter.catalog.ManagedCarRuntimeState.NotRunning => "not-running"; case state => state.toString.toLowerCase }), "activeInstanceIds" -> runtime.activeInstanceIds, "staleInstanceIds" -> runtime.staleInstanceIds, "sources" -> sources.map(source => Record.dataAuto("sourceId" -> source.sourceId.value, "sourceKind" -> source.sourceKind.value, "refreshState" -> source.refreshState.value, "componentName" -> source.componentName.map(_.value), "recommendedVersion" -> source.recommendedVersion.map(_.value), "latestVersion" -> source.latestVersion.map(_.value), "snapshotAt" -> source.snapshotAt, "diagnostic" -> source.diagnostic.map(_.value), "privateLocator" -> (if (detail) source.privateLocator.map(_.value) else None))))
    protected final def matches_text(car: ManagedCarEntity, text: String): Boolean = Vector(car.artifactId.value).concat(car.componentName.map(_.value).toVector).exists(_.toLowerCase.contains(text))
  }
}
