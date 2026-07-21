/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OperationalComponentManagementSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Operational component management" should {
    "auto-manage a development CAR and retain it when its source is temporarily unavailable" in {
      Given("a development CAR with no existing operating-target record")
      val initial = OperationalComponentManagement.reconcile(None, developmentsourceavailable = true, accepteduseevidence = false, exclusionrecordexists = false)

      When("a later catalog refresh cannot see the same development source")
      val retained = OperationalComponentManagement.reconcile(initial, developmentsourceavailable = false, accepteduseevidence = false, exclusionrecordexists = false)

      Then("the operating target remains auto-managed rather than disappearing")
      initial shouldBe Some(OperationalManagementState.AutoManaged)
      retained shouldBe Some(OperationalManagementState.AutoManaged)
    }

    "adopt a repository CAR only after accepted launcher use evidence exists" in {
      Given("a repository CAR without development availability")
      val beforeUse = OperationalComponentManagement.reconcile(None, developmentsourceavailable = false, accepteduseevidence = false, exclusionrecordexists = false)

      When("the launcher reports accepted use of the CAR")
      val afterUse = OperationalComponentManagement.reconcile(beforeUse, developmentsourceavailable = false, accepteduseevidence = true, exclusionrecordexists = false)

      Then("the CAR becomes an adopted operating target")
      beforeUse shouldBe None
      afterUse shouldBe Some(OperationalManagementState.Adopted)
    }

    "retain an explicit exclusion ahead of automatic discovery and hide it from the operating panel" in {
      Given("an auto-managed development CAR")
      val managed = OperationalComponentManagement.reconcile(None, developmentsourceavailable = true, accepteduseevidence = false, exclusionrecordexists = false)

      When("the operator removes it from management while its source remains available")
      val excluded = OperationalComponentManagement.reconcile(managed, developmentsourceavailable = true, accepteduseevidence = false, exclusionrecordexists = true)

      And("a later development refresh sees the same source while the exclusion record remains")
      val retained = OperationalComponentManagement.reconcile(excluded, developmentsourceavailable = true, accepteduseevidence = false, exclusionrecordexists = true)

      Then("the exclusion is retained across refresh and the record is not visible in the normal panel")
      excluded shouldBe Some(OperationalManagementState.Excluded)
      retained shouldBe Some(OperationalManagementState.Excluded)
      excluded.map(OperationalComponentManagement.visible) shouldBe Some(false)
    }
  }
}
