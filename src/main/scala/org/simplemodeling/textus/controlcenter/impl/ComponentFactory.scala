/*
 *  version Jul. 28, 2026
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
