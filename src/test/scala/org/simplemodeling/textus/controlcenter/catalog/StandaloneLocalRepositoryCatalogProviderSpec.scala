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
      Given("a configured local catalog with one archived CAR and unrelated metadata")
      val localroot = Files.createTempDirectory("local-catalog")
      val root = Files.createDirectories(localroot.resolve("repository/catalog/car"))
      val archive = localroot.resolve("repository/car/textus-sample/1.1.0/textus-sample-1.1.0.car")
      Files.createDirectories(archive.getParent)
      Files.writeString(archive, "CAR")
      Files.writeString(root.resolve("textus-sample.yaml"), "kind: car\nartifactId: textus-sample\nrecommended: 1.0.0\nlatestStable: 1.1.0\nversions:\n  - version: 1.1.0\n    file: repository/car/textus-sample/1.1.0/textus-sample-1.1.0.car\n")
      Files.writeString(root.resolve("ignored.model-metadata.yaml"), "kind: car\nartifactId: ignored\n")

      When("the local repository provider reads the configured catalog root")
      val sources = StandaloneLocalRepositoryCatalogProvider.discover(LocalRepositoryCatalog("local", root.toString), Instant.parse("2026-07-21T00:00:00Z"))

      Then("one local source exposes only an existing archive version without a runtime state")
      sources.map(_.artifactId) shouldBe Vector("textus-sample")
      sources.head.availableVersions shouldBe Vector("1.1.0")
      sources.head.refreshState shouldBe ManagedCarRefreshState.Available
      sources.head.privateLocator shouldBe Some(root.resolve("textus-sample.yaml").toString)
    }

    "retain an unavailable local catalog record when its declared archive is absent" in {
      Given("a local catalog record whose referenced archive is unavailable")
      val localroot = Files.createTempDirectory("local-catalog-unavailable")
      val root = Files.createDirectories(localroot.resolve("repository/catalog/car"))
      Files.writeString(root.resolve("textus-sample.yaml"), "kind: car\nartifactId: textus-sample\nversions:\n  - version: 1.1.0\n    file: repository/car/textus-sample/1.1.0/textus-sample-1.1.0.car\n")

      When("the provider reads the configured local catalog")
      val sources = StandaloneLocalRepositoryCatalogProvider.discover(LocalRepositoryCatalog("local", root.toString), Instant.parse("2026-07-21T00:00:00Z"))

      Then("the logical CAR remains observable with an archive availability diagnostic")
      sources.map(_.artifactId) shouldBe Vector("textus-sample")
      sources.head.refreshState shouldBe ManagedCarRefreshState.Unavailable
      sources.head.availableVersions shouldBe Vector.empty
      sources.head.diagnostic shouldBe Some("local-car-archive-unavailable")
    }
  }
}
