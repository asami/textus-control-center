/*
 * @version Jul. 18, 2026
 */
package org.simplemodeling.textus.admin

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ComponentFactorySpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Textus Admin component factory" should {
    "expose the generated primary component factory" in {
      Given("the Cozy-generated Textus Admin bundle factory")
      val factory = new impl.ComponentFactory()

      When("the runtime asks for its primary factory")
      val primary = factory.primaryFactory

      Then("the factory provides the Textus Admin primary component")
      primary shouldBe impl.TextusAdminPrimaryFactory
    }
  }
}
