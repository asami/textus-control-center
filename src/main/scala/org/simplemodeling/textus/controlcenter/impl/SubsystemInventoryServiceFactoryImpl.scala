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
        protocolVersion = protocolversion,
        instanceId = instanceid,
        launcherKind = launcherkind,
        target = target,
        artifactId = record.getString("artifactId").map(_.trim).filter(_.nonEmpty),
        executionMode = record.getString("executionMode").map(_.trim).filter(_.nonEmpty),
        developmentDirectory = record.getString("developmentDirectory").map(_.trim).filter(_.nonEmpty),
        subsystemName = record.getString("subsystemName").map(_.trim).filter(_.nonEmpty),
        subsystemVersion = record.getString("subsystemVersion").map(_.trim).filter(_.nonEmpty),
        runtimeVersion = record.getString("runtimeVersion").map(_.trim).filter(_.nonEmpty),
        baseUrl = baseurl,
        hostLabel = hostlabel,
        startedAt = startedat,
        launcherState = launcherstate,
        applicationUrl = record.getString("applicationUrl").map(_.trim).filter(_.nonEmpty)
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
        applicationUrl = source.applicationUrl.flatMap(value => ApplicationUrlPolicy.parse(value.toExternalForm).toOption.map(_.toString))
      )

    protected final def to_create(
      source: RegistrySubsystem
    ): Consequence[RegisteredSubsystemCreate] =
      for {
        baseurl <- base_url(source.baseUrl)
        applicationurl <- source.applicationUrl.traverse(application_url)
      } yield {
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
          applicationurl,
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
        create.applicationUrl,
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

    protected final def application_url(value: String): Consequence[URL] =
      ApplicationUrlPolicy.parse(value).fold(
        _ => Consequence.valueInvalid(s"applicationUrl is invalid: $value", XString),
        uri => Consequence.success(uri.toURL)
      )

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
          "applicationUrl" -> projection.applicationUrl,
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

object SubsystemInventoryServiceFactoryImpl {
  def apply(): SubsystemInventoryServiceFactoryImpl = new SubsystemInventoryServiceFactoryImpl()
}
