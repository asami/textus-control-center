/*
 * @version Jul. 18, 2026
 */
package org.simplemodeling.textus.admin.impl

import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.simplemodeling.textus.admin.TextusAdminComponent

final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    TextusAdminPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class TextusAdminParticipantFactoryBase extends TextusAdminComponent.Factory {
  protected final val shared_services =
    Vector(
      TextusAdminComponent.SubsystemInventoryService,
      TextusAdminComponent.AggregateService,
      TextusAdminComponent.ViewService,
      TextusAdminComponent.EntityService
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
    RegisterSubsystemActionCall(core, action)

  override def createHeartbeatSubsystemActionCall(
    core: ActionCall.Core,
    action: HeartbeatSubsystem
  ): HeartbeatSubsystemActionCall =
    HeartbeatSubsystemActionCall(core, action)

  override def createDeregisterSubsystemActionCall(
    core: ActionCall.Core,
    action: DeregisterSubsystem
  ): DeregisterSubsystemActionCall =
    DeregisterSubsystemActionCall(core, action)

  override def createListSubsystemsActionCall(
    core: ActionCall.Core,
    action: ListSubsystems
  ): ListSubsystemsActionCall =
    ListSubsystemsActionCall(core, action)

  override def createGetSubsystemActionCall(
    core: ActionCall.Core,
    action: GetSubsystem
  ): GetSubsystemActionCall =
    GetSubsystemActionCall(core, action)
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
