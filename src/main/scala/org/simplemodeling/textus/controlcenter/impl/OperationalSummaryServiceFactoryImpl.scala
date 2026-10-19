/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
package org.simplemodeling.textus.controlcenter.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.component.Component
import org.goldenport.cncf.context.{GlobalRuntimeContext, ScopeContext, SecurityContext}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.{EntityQuery, EntitySearchScope, EntityVisibilityScope}
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.goldenport.record.io.RecordEncoder
import org.simplemodeling.textus.controlcenter.TextusControlCenterComponent
import org.simplemodeling.textus.controlcenter.entity.{RegisteredSubsystem as RegisteredSubsystemEntity}
import org.simplemodeling.textus.controlcenter.entity.query.{RegisteredSubsystem as RegisteredSubsystemQuery}
import org.simplemodeling.textus.controlcenter.registry.RegisteredSubsystem
import org.simplemodeling.textus.controlcenter.summary.OperationalSummary

final class OperationalSummaryServiceFactoryImpl extends TextusControlCenterComponent.OperationalSummaryServiceFactory {
  import TextusControlCenterComponent.OperationalSummaryService.*

  override def createGetSummaryActionCall(core: ActionCall.Core, action: GetSummary): GetSummaryActionCall =
    GetSummaryActionCallImpl(core, action)

  private final case class GetSummaryActionCallImpl(
    core: ActionCall.Core,
    override val action: GetSummary
  ) extends GetSummaryActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(_authorize)
        limit <- exec_from(OperationalSummary.createLimitC(action.record.getInt("limit")))
        component <- exec_from(core.component.toRight("Operational Summary requires the current component context")
          .fold(Consequence.operationInvalid, Consequence.success))
        now = current_instant
        version <- exec_from(_component_version(component))
        sources <- _find_registered
        result <- exec_from(OperationalSummary.project(
          component.core.componentId.toString,
          component.core.instanceId.toString,
          version,
          _runtime_metadata(executionContext.scope),
          component.healthSnapshot,
          sources.map(_stored_instance),
          now,
          limit
        ))
        response <- exec_from(_summary_response(result))
      } yield response

    private def _summary_response(record: Record): Consequence[OperationResponse] =
      Consequence(RecordEncoder.json(record)).flatMap { text =>
        io.circe.parser.parse(text).fold(
          _ => Consequence.operationInvalid("Operational Summary could not encode its fixed JSON contract"),
          json => Consequence.success(OperationResponse.Json(json))
        )
      }

    private def _authorize: Consequence[Unit] = {
      val subject = SecuritySubject.current(using executionContext)
      val privileges = Vector(
        SecurityContext.Privilege.ApplicationContentManager,
        SecurityContext.Privilege.Operator,
        SecurityContext.Privilege.System,
        SecurityContext.Privilege.Internal
      )
      if (subject.isAuthenticated && privileges.exists { privilege =>
        subject.hasPrivilege(privilege.name) || subject.hasCapability(privilege.name) || subject.hasRole(privilege.name)
      }) Consequence.unit
      else Consequence.securityPermissionDenied("Operational Summary requires administrative inventory authorization.")
    }

    private def _component_version(component: Component): Consequence[Option[String]] =
      component.logic.makeOperationRequest(Request.ofService("meta", "version")).flatMap {
        case action: Action =>
          component.actionEngine.execute(component.logic.createActionCall(action, executionContext)).flatMap {
            case OperationResponse.RecordResponse(record) => Consequence.success(record.getString("version"))
            case _ => Consequence.operationInvalid("component meta.version did not return a metadata record")
          }
        case _ => Consequence.operationInvalid("component meta.version did not resolve to an action")
      }

    private def _find_registered: ExecUowM[Vector[RegisteredSubsystemEntity]] =
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

    @annotation.tailrec
    private def _runtime_metadata(scope: ScopeContext): Option[OperationalSummary.RuntimeMetadata] =
      scope match {
        case global: GlobalRuntimeContext =>
          Some(OperationalSummary.RuntimeMetadata(global.bootedAt, global.runtimeVersion))
        case other => other.parent match {
          case Some(parent) => _runtime_metadata(parent)
          case None => None
        }
      }

    private def _stored_instance(source: RegisteredSubsystemEntity): OperationalSummary.StoredInstance =
      OperationalSummary.StoredInstance(source.id.print, RegisteredSubsystem(
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
      ))
  }
}

object OperationalSummaryServiceFactoryImpl {
  def apply(): OperationalSummaryServiceFactoryImpl = new OperationalSummaryServiceFactoryImpl()
}
