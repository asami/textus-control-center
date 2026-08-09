/*
 * @version Aug.  9, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StandaloneDevelopmentCatalogProviderSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Standalone development catalog provider" should {
    "read canonical first-level textus CAR descriptors from the configured root" in {
      Given("a configured root with one canonical ArtScene CAR, one legacy-only textus CAR, one ordinary project, and one nested fixture")
      val root = Files.createTempDirectory("catalog-provider")
      val car = Files.createDirectory(root.resolve("textus-art-scene"))
      Files.writeString(car.resolve("project.yaml"), """project:
  namespace: "org.simplemodeling.textus"
  id: "ArtScene"
  title: "Textus Art Scene"
  kind: car
  component:
    displayName: "Textus Art Scene"
    version: "0.1.2-SNAPSHOT"
""", StandardCharsets.UTF_8)
      val legacy = Files.createDirectory(root.resolve("textus-legacy"))
      Files.writeString(legacy.resolve("project.yaml"), "project:\n  name: textus-legacy\n  kind: car\n", StandardCharsets.UTF_8)
      val ordinary = Files.createDirectory(root.resolve("ordinary-project"))
      Files.writeString(ordinary.resolve("project.yaml"), "project:\n  name: ordinary\n  kind: car\n", StandardCharsets.UTF_8)
      val fixture = Files.createDirectories(root.resolve("fixtures/textus-fixture"))
      Files.writeString(fixture.resolve("project.yaml"), "project:\n  name: textus-fixture\n  kind: car\n", StandardCharsets.UTF_8)

      When("the provider discovers configured development projects")
      val sources = StandaloneDevelopmentCatalogProvider.discover(DevelopmentRoot("dev", root.toString), Instant.parse("2026-07-21T00:00:00Z"))

      Then("only the direct canonical textus CAR is returned with its qualified component and version")
      sources.map(_.artifactId) shouldBe Vector("textus-art-scene")
      sources.head.componentName shouldBe Some("org.simplemodeling.textus.ArtScene")
      sources.head.availableVersions shouldBe Vector("0.1.2-SNAPSHOT")
      sources.head.privateLocator shouldBe Some(car.toString)
    }
  }
}
