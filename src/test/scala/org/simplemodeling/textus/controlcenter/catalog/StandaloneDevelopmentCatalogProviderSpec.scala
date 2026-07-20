/*
 * @version Jul. 21, 2026
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
    "read only first-level textus CAR descriptors from the configured root" in {
      Given("a configured root with one CAR, one ordinary project, and one nested fixture")
      val root = Files.createTempDirectory("catalog-provider")
      val car = Files.createDirectory(root.resolve("textus-sample"))
      Files.writeString(car.resolve("project.yaml"), "project:\n  name: textus-sample\n  kind: car\n  component:\n    name: sample-component\n", StandardCharsets.UTF_8)
      val ordinary = Files.createDirectory(root.resolve("ordinary-project"))
      Files.writeString(ordinary.resolve("project.yaml"), "project:\n  name: ordinary\n  kind: car\n", StandardCharsets.UTF_8)
      val fixture = Files.createDirectories(root.resolve("fixtures/textus-fixture"))
      Files.writeString(fixture.resolve("project.yaml"), "project:\n  name: textus-fixture\n  kind: car\n", StandardCharsets.UTF_8)

      When("the provider discovers configured development projects")
      val sources = StandaloneDevelopmentCatalogProvider.discover(DevelopmentRoot("dev", root.toString), Instant.parse("2026-07-21T00:00:00Z"))

      Then("only the direct textus CAR is returned with a protected locator")
      sources.map(_.artifactId) shouldBe Vector("textus-sample")
      sources.head.componentName shouldBe Some("sample-component")
      sources.head.privateLocator shouldBe Some(car.toString)
    }
  }
}
