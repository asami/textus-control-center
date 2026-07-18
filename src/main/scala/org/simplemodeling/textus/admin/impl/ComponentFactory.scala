/*
 * @version Jul. 18, 2026
 */
package org.simplemodeling.textus.admin.impl

import java.time.{Duration, Instant}

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityQuery, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.context.SecurityContext
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.admin.TextusAdminComponent
import org.simplemodeling.textus.admin.entity.{RegisteredSubsystem as RegisteredSubsystemEntity}
import org.simplemodeling.textus.admin.entity.create.{RegisteredSubsystem as RegisteredSubsystemCreate}
import org.simplemodeling.textus.admin.entity.create.RegisteredSubsystem.given
import org.simplemodeling.textus.admin.entity.query.{RegisteredSubsystem as RegisteredSubsystemQuery}
import org.simplemodeling.textus.admin.registry.{RegisteredSubsystem as RegistrySubsystem, RegistryError, RegistrationInput, SubsystemRegistry}

final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    TextusAdminPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class TextusAdminParticipantFactoryBase extends TextusAdminComponent.Factory {
  protected final val shared_services =
    Vector(
      TextusAdminComponent.SubsystemInventoryService
    )

  protected final def component_core(
    name: String,
    componentid: ComponentId
  ): Component.Core =
    spec_create(name, componentid, shared_services)

  override val SubsystemInventory: TextusAdminComponent.SubsystemInventoryServiceFactory =
    SubsystemInventoryServiceFactoryImpl()
  override val aggregate: TextusAdminComponent.AggregateServiceFactory =
    AggregateServiceFactoryImpl()
  override val view: TextusAdminComponent.ViewServiceFactory =
    ViewServiceFactoryImpl()
  override val entity: TextusAdminComponent.EntityServiceFactory =
    EntityServiceFactoryImpl()
}

final class TextusAdminPrimaryComponent extends TextusAdminComponent

object TextusAdminPrimaryFactory extends TextusAdminParticipantFactoryBase with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component =
    new TextusAdminPrimaryComponent()

  override protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core =
    component_core(TextusAdminComponent.name, TextusAdminComponent.componentId)
}

final class SubsystemInventoryServiceFactoryImpl extends TextusAdminComponent.SubsystemInventoryServiceFactory {
  import TextusAdminComponent.SubsystemInventoryService.*

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
      if (subject.isAuthenticated && executionContext.security.subjectKind != org.goldenport.cncf.context.SubjectKind.Anonymous)
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
      if (subject.isAuthenticated && privileges.exists(privilege => subject.hasPrivilege(privilege.name)))
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

final class EntityServiceFactoryImpl extends TextusAdminComponent.EntityServiceFactory

object EntityServiceFactoryImpl {
  def apply(): EntityServiceFactoryImpl = new EntityServiceFactoryImpl()
}

final class AggregateServiceFactoryImpl extends TextusAdminComponent.AggregateServiceFactory

object AggregateServiceFactoryImpl {
  def apply(): AggregateServiceFactoryImpl = new AggregateServiceFactoryImpl()
}

final class ViewServiceFactoryImpl extends TextusAdminComponent.ViewServiceFactory

object ViewServiceFactoryImpl {
  def apply(): ViewServiceFactoryImpl = new ViewServiceFactoryImpl()
}
