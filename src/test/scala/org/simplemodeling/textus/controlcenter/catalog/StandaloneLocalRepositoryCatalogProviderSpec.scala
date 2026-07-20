/*
 * @version Jul. 21, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.nio.file.Files
import java.time.Instant
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StandaloneLocalRepositoryCatalogProviderSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Standalone local repository catalog provider" should {
    "read configured CAR records and retain only local package facts" in {
      Given("a configured local catalog with one CAR and unrelated metadata")
      val root = Files.createTempDirectory("local-catalog")
      Files.writeString(root.resolve("textus-sample.yaml"), "kind: car\nartifactId: textus-sample\nrecommended: 1.0.0\nlatestStable: 1.1.0\n")
      Files.writeString(root.resolve("ignored.model-metadata.yaml"), "kind: car\nartifactId: ignored\n")

      When("the local repository provider reads the configured catalog root")
      val sources = StandaloneLocalRepositoryCatalogProvider.discover(LocalRepositoryCatalog("local", root.toString), Instant.parse("2026-07-21T00:00:00Z"))

      Then("one local source exposes identity and version facts without a runtime state")
      sources.map(_.artifactId) shouldBe Vector("textus-sample")
      sources.head.availableVersions shouldBe Vector("1.0.0", "1.1.0")
      sources.head.privateLocator shouldBe Some(root.resolve("textus-sample.yaml").toString)
    }
  }
}
