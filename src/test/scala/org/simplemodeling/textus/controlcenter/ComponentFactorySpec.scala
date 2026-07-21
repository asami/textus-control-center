/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter

import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, SubjectKind}
import org.goldenport.cncf.security.AuthenticationRequest
import org.goldenport.cncf.subsystem.Subsystem
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ComponentFactorySpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Textus Control Center component factory" should {
    "expose the generated primary component factory" in {
      Given("the Cozy-generated Textus Control Center bundle factory")
      val factory = new impl.ComponentFactory()

      When("the runtime asks for its primary factory")
      val primary = factory.primaryFactory

      Then("the factory provides the Textus Control Center primary component")
      primary shouldBe impl.TextusControlCenterPrimaryFactory
    }

    "publish inventory, catalog, operational management, and lifecycle control services rather than generic Entity operations" in {
      Given("a Textus Control Center component assembled into a subsystem")
      val component = _component()

      When("CNCF projects the component service boundary")
      val services = component.protocol.services.services.map(_.name)

      Then("the four administrative domain services are published")
      services.filterNot(name => Set("meta", "system").contains(name)) shouldBe
        Vector(
          TextusControlCenterComponent.SubsystemInventoryService.name,
          TextusControlCenterComponent.CarCatalogService.name,
          TextusControlCenterComponent.OperationalManagementService.name,
          TextusControlCenterComponent.LifecycleControlService.name
        )
    }

    "authenticate a configured launcher token as a service without administration capability" in {
      Given("a Textus Control Center component with a server-side launcher credential")
      val configuration = Configuration(Map(
        "textus-control-center.registration.authentication.token" -> ConfigurationValue.StringValue("component-factory-test-token"),
        "textus-control-center.registration.authentication.principal-id" -> ConfigurationValue.StringValue("component-factory-launcher")
      ))
      val component = _component(configuration)
      val provider = component.authenticationProviders.head
      given ExecutionContext = ExecutionContext.create()

      When("the provider receives the matching bearer token")
      val result = provider.authenticate(AuthenticationRequest(Map("authorization" -> "Bearer component-factory-test-token")))

      Then("it authenticates only the configured launcher service principal")
      val authentication = result.toOption.flatten.getOrElse(fail("launcher credential was not accepted"))
      authentication.principalId.value shouldBe "component-factory-launcher"
      authentication.subjectKind shouldBe SubjectKind.Service
      authentication.capabilities.map(_.name) shouldBe Set("launcher_registration")

      And("an unrecognized bearer token is not authenticated by this provider")
      provider.authenticate(AuthenticationRequest(Map("authorization" -> "Bearer unrelated-token"))).toOption.flatten shouldBe empty
    }
  }

  private def _component(configuration: Configuration = Configuration.empty): Component = {
    val subsystem = Subsystem(
      name = "textus-control-center-component-factory-spec",
      configuration = ResolvedConfiguration(configuration, ConfigurationTrace.empty)
    )
    val bundle = new impl.ComponentFactory().create(ComponentCreate(subsystem, ComponentOrigin.Main))
    subsystem.add(bundle)
    subsystem.components.find(_.name == TextusControlCenterComponent.name).getOrElse(
      fail("Textus Control Center component is missing")
    )
  }
}
