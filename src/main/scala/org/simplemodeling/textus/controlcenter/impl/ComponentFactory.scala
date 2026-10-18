/*
 *  version Jul. 28, 2026
 *  version Aug. 28, 2026
 * @version Oct. 18, 2026
 */
package org.simplemodeling.textus.controlcenter.impl

import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.event.{CmlEventCategory, CmlEventDefinition, CmlSubscriptionDefinition, DispatchRoute, EventOriginBoundary, EventReceptionCondition, EventReceptionExecutionPolicy, EventReceptionRule}
import org.goldenport.cncf.security.AuthenticationProvider
import org.goldenport.cncf.spi.supervisor.SupervisorSocket
import org.simplemodeling.textus.controlcenter.TextusControlCenterComponent

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
      , TextusControlCenterComponent.OperationalSummaryService
    )

  protected final def component_core(
    name: String,
    componentId: ComponentId
  ): Component.Core =
    spec_create(name, componentId, shared_services)

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
  override val OperationalSummary: TextusControlCenterComponent.OperationalSummaryServiceFactory =
    OperationalSummaryServiceFactoryImpl()
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
