/*
 *  version Jul. 24, 2026
 * @version Aug. 10, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.time.Instant
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StandalonePublicRepositoryCatalogProviderSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Standalone public catalog provider" should {
    "resolve only an explicitly subscribed artifact catalog and parse its safe facts" in {
      Given("a SimpleModeling.org repository subscription and one artifact catalog document")
      val repository = PublicRepositoryCatalog("simplemodeling", "https://www.simplemodeling.org/repository/catalog/car", Vector("textus-user-account"))
      val document = """schemaVersion: 1
        |kind: car
        |artifactId: textus-user-account
        |recommended: 0.1.2
        |latestStable: 0.1.4
        |versions:
        |  - version: 0.1.4
        |    component: account-component
        |""".stripMargin

      When("the subscription response is parsed")
      val source = StandalonePublicRepositoryCatalogProvider.available(repository, "textus-user-account", document, Instant.parse("2026-07-21T00:00:00Z"))

      Then("it produces no archive download or locator and retains the published version facts")
      StandalonePublicRepositoryCatalogProvider.catalog_url(repository, "textus-user-account") shouldBe "https://www.simplemodeling.org/repository/catalog/car/textus-user-account.yaml"
      source.refreshState shouldBe ManagedCarRefreshState.Available
      source.componentName shouldBe Some("account-component")
      source.availableVersions shouldBe Vector("0.1.2", "0.1.4")
      source.privateLocator shouldBe None
    }

    "record unavailable and malformed responses without fabricating catalog facts" in {
      Given("an explicit public subscription")
      val repository = PublicRepositoryCatalog("simplemodeling", "https://www.simplemodeling.org/repository/catalog/car", Vector("textus-user-account"))
      val observedAt = Instant.parse("2026-07-21T00:00:00Z")

      When("HTTP or catalog validation fails")
      val unavailable = StandalonePublicRepositoryCatalogProvider.unavailable(repository, "textus-user-account", observedAt)
      val invalid = StandalonePublicRepositoryCatalogProvider.available(repository, "textus-user-account", "kind: car\nartifactId: other-artifact\n", observedAt)

      Then("the source state and safe diagnostic expose the failure")
      unavailable.refreshState shouldBe ManagedCarRefreshState.Unavailable
      unavailable.diagnostic shouldBe Some("public-catalog-unavailable")
      invalid.refreshState shouldBe ManagedCarRefreshState.Invalid
      invalid.diagnostic shouldBe Some("invalid-public-catalog")
      invalid.availableVersions shouldBe Vector.empty
    }

    "parse the versioned global index into deterministic safe CAR identifiers" in {
      Given("the official component repository index")
      val repository = PublicRepositoryCatalog("simplemodeling", "https://www.simplemodeling.org/repository/catalog/car", Vector.empty)
      When("the public source parses its structural JSON entries")
      val ids = StandalonePublicRepositoryCatalogProvider.indexArtifactIds("""{"schema":"cncf.component-repository-index.v1","artifacts":[{"artifactId":"textus-blog"},{"artifactId":"textus-user-account"}]}""")
      Then("the index URL and safe artifact candidates are exact")
      StandalonePublicRepositoryCatalogProvider.indexUrl(repository) shouldBe "https://www.simplemodeling.org/repository/catalog/index.json"
      ids shouldBe Right(Vector("textus-blog", "textus-user-account"))
    }

    "reject an invalid empty-subscription index without creating a candidate artifact" in {
      Given("a public repository configured for index discovery without explicit subscriptions")
      val repository = PublicRepositoryCatalog("simplemodeling", "https://www.simplemodeling.org/repository/catalog/car", Vector.empty)

      When("its versioned index response is malformed")
      val candidates = StandalonePublicRepositoryCatalogProvider.indexArtifactIds("not a catalog index")

      Then("the refresh path has only the safe index diagnostic and no fabricated artifact candidate")
      repository.subscriptions shouldBe empty
      candidates shouldBe Left("public-index-invalid")
    }
  }
}
