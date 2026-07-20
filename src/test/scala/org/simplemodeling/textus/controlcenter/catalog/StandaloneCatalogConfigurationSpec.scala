/*
 * @version Jul. 21, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.time.Duration

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StandaloneCatalogConfigurationSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Standalone catalog configuration" should {
    "prefer an explicitly selected standalone configuration over the default location" in {
      Given("a default empty installation configuration and an explicit source configuration")
      val default = StandaloneCatalogConfiguration(Vector.empty, None, Vector.empty, Duration.ofSeconds(5))
      val explicit = _configuration(developmentRoots = Vector(DevelopmentRoot("dev2026", "/work/src/dev2026")))

      When("the standalone profile resolves its configuration")
      val resolved = StandaloneCatalogConfiguration.resolve(Some(explicit), default)

      Then("the explicit configuration owns the selected source roots")
      resolved shouldBe explicit
    }

    "accept only explicit public artifact subscriptions" in {
      Given("a configured SimpleModeling.org catalog with two subscriptions")
      val configuration = _configuration(
        publicRepositoryCatalogs = Vector(
          PublicRepositoryCatalog(
            "simplemodeling",
            "https://www.simplemodeling.org/repository/catalog/car",
            Vector("textus-blog", "textus-user-account")
          )
        )
      )

      When("the configuration is validated and queried")
      val valid = StandaloneCatalogConfiguration.validate(configuration)

      Then("known subscriptions are admitted without a global repository enumeration")
      valid shouldBe Right(configuration)
      configuration.isSubscribed("textus-blog") shouldBe true
      configuration.isSubscribed("textus-unsubscribed") shouldBe false
    }

    "reject relative roots, unbounded refreshes, and duplicate public subscriptions" in {
      Given("invalid standalone source declarations")
      val relative = _configuration(developmentRoots = Vector(DevelopmentRoot("dev", "src/dev2026")))
      val unbounded = _configuration(refreshTimeout = Duration.ofSeconds(31))
      val duplicate = _configuration(
        publicRepositoryCatalogs = Vector(
          PublicRepositoryCatalog(
            "simplemodeling",
            "https://www.simplemodeling.org/repository/catalog/car",
            Vector("textus-blog", "textus-blog")
          )
        )
      )

      When("the configuration validator examines each declaration")
      val relativeResult = StandaloneCatalogConfiguration.validate(relative)
      val unboundedResult = StandaloneCatalogConfiguration.validate(unbounded)
      val duplicateResult = StandaloneCatalogConfiguration.validate(duplicate)

      Then("it fails before a refresh adapter can scan or call a source")
      relativeResult.left.map(_.message) shouldBe Left("development roots must use absolute paths and the textus- include prefix")
      unboundedResult.left.map(_.message) shouldBe Left("refreshTimeout must not exceed 30 seconds")
      duplicateResult.left.map(_.message) shouldBe Left("public repositories require HTTPS catalog bases and unique explicit artifact subscriptions")
    }

    "reserve fixture and nested example inclusion for explicit artifact selection" in {
      Given("a first-level development root with the canonical textus- prefix")
      val root = DevelopmentRoot(
        "dev2026",
        "/work/src/dev2026",
        explicitProjects = Vector("textus-control-center")
      )
      val configuration = _configuration(developmentRoots = Vector(root))

      When("the root configuration is validated")
      val validated = StandaloneCatalogConfiguration.validate(configuration)

      Then("the adapter contract receives only an explicit first-level selection policy")
      validated shouldBe Right(configuration)
      root.includePrefix shouldBe StandaloneCatalogConfiguration.DEVELOPMENT_INCLUDE_PREFIX
      root.explicitProjects shouldBe Vector("textus-control-center")
    }
  }

  private def _configuration(
    developmentRoots: Vector[DevelopmentRoot] = Vector.empty,
    localRepositoryCatalog: Option[LocalRepositoryCatalog] = None,
    publicRepositoryCatalogs: Vector[PublicRepositoryCatalog] = Vector.empty,
    refreshTimeout: Duration = Duration.ofSeconds(5)
  ): StandaloneCatalogConfiguration =
    StandaloneCatalogConfiguration(
      developmentRoots,
      localRepositoryCatalog,
      publicRepositoryCatalogs,
      refreshTimeout
    )
}
