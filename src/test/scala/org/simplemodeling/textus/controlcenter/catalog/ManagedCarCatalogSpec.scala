/*
 * @version Jul. 21, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.time.Instant

import org.scalacheck.{Gen, Prop, Test as ScalaCheckTest}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ManagedCarCatalogSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "Managed CAR catalog" should {
    "merge source snapshots by descriptor artifact identity rather than development directory" in {
      Given("development, local, and public snapshots for one artifact from different locators")
      val sources = Vector(
        _source("textus-user-account", ManagedCarSourceKind.PublicRepository, "public", Some("account-component"), None),
        _source("textus-user-account", ManagedCarSourceKind.Development, "dev", Some("account-component"), Some("/work/renamed-project")),
        _source("textus-user-account", ManagedCarSourceKind.LocalRepository, "local", Some("account-component"), Some("/cncf/catalog"))
      )

      When("the catalog merges the source snapshots")
      val cars = ManagedCarCatalog.mergeSources(sources)

      Then("one CAR has all sources in deterministic source order")
      cars.map(_.artifactId) shouldBe Vector("textus-user-account")
      cars.head.sources.map(_.sourceKind) shouldBe Vector(
        ManagedCarSourceKind.Development,
        ManagedCarSourceKind.LocalRepository,
        ManagedCarSourceKind.PublicRepository
      )
    }

    "retain a source snapshot and diagnostic when a later refresh is unavailable" in {
      Given("a last successful local source and a later unavailable snapshot")
      val available = _source("textus-blog", ManagedCarSourceKind.LocalRepository, "local", Some("blog-component"), Some("/cncf/catalog"))
      val unavailable = available.copy(
        refreshState = ManagedCarRefreshState.Unavailable,
        diagnostic = Some("repository-unavailable")
      )

      When("the persisted source snapshots are projected")
      val projection = ManagedCarCatalog.sourceProjection(unavailable)

      Then("the operation-facing value has retained version facts and no private locator")
      projection.availableVersions shouldBe available.availableVersions
      projection.refreshState shouldBe ManagedCarRefreshState.Unavailable
      projection.diagnostic shouldBe Some("repository-unavailable")
      projection.productElementNames.toSet should not contain "privateLocator"
    }

    "report conflicting component identities without selecting one source as authoritative" in {
      Given("two snapshots for one artifact that disagree on the component identity")
      val sources = Vector(
        _source("textus-scraper", ManagedCarSourceKind.Development, "dev", Some("scraper-component"), None),
        _source("textus-scraper", ManagedCarSourceKind.LocalRepository, "local", Some("other-component"), None)
      )

      When("the catalog merges the snapshots")
      val car = ManagedCarCatalog.mergeSources(sources).head

      Then("it retains both sources, records a safe diagnostic, and does not guess a component identity")
      car.sources.size shouldBe 2
      car.componentName shouldBe None
      car.diagnostics shouldBe Vector("conflicting-component-identity")
    }

    "link runtime instances directly by artifact identity and preserve legacy compatibility" in {
      Given("two managed CARs and current plus legacy launcher reports")
      val cars = ManagedCarCatalog.mergeSources(
        Vector(
          _source("textus-blog", ManagedCarSourceKind.Development, "dev-blog", Some("blog-component"), None),
          _source("textus-user-account", ManagedCarSourceKind.Development, "dev-account", Some("account-component"), None)
        ),
        Map("textus-blog" -> Set("blog-component"), "textus-user-account" -> Set("account-component"))
      )
      val instances = Vector(
        RuntimeInstance("direct", Some("textus-blog"), "current-project", None, RuntimeInstanceStatus.Running),
        RuntimeInstance("legacy", None, "current-project", Some("account-component"), RuntimeInstanceStatus.Starting),
        RuntimeInstance("unknown", Some("textus-missing"), "current-project", None, RuntimeInstanceStatus.Running)
      )

      When("the catalog correlates runtime instances")
      val links = ManagedCarCatalog.linkRuntimeInstances(cars, instances)

      Then("direct identity wins, one explicit legacy alias is compatible, and unknown identity stays unlinked")
      links shouldBe Vector(
        RuntimeLink("direct", Some("textus-blog"), RuntimeLinkKind.Direct, None),
        RuntimeLink("legacy", Some("textus-user-account"), RuntimeLinkKind.Legacy, None),
        RuntimeLink("unknown", None, RuntimeLinkKind.Unlinked, Some("unmanaged-artifact-id"))
      )
    }

    "leave ambiguous legacy reports unlinked without guessing from a directory name" in {
      Given("two CARs that intentionally share one configured legacy alias")
      val cars = ManagedCarCatalog.mergeSources(
        Vector(
          _source("textus-alpha", ManagedCarSourceKind.Development, "alpha", Some("shared-component"), None),
          _source("textus-beta", ManagedCarSourceKind.Development, "beta", Some("shared-component"), None)
        ),
        Map("textus-alpha" -> Set("shared-component"), "textus-beta" -> Set("shared-component"))
      )
      val instance = RuntimeInstance("ambiguous", None, "current-project", Some("shared-component"), RuntimeInstanceStatus.Running)

      When("the catalog attempts its documented legacy fallback")
      val link = ManagedCarCatalog.linkRuntimeInstances(cars, Vector(instance)).head

      Then("it emits an ambiguity diagnostic rather than selecting a CAR")
      link shouldBe RuntimeLink("ambiguous", None, RuntimeLinkKind.Ambiguous, Some("ambiguous-legacy-runtime-link"))
    }

    "show a managed CAR with only stale instances as not running while retaining the stale instance fact" in {
      Given("a managed CAR linked to one stale launcher instance")
      val cars = ManagedCarCatalog.mergeSources(Vector(_source("textus-scraper", ManagedCarSourceKind.PublicRepository, "public", Some("scraper-component"), None)))
      val instance = RuntimeInstance("stale-instance", Some("textus-scraper"), "textus-scraper", None, RuntimeInstanceStatus.Stale)
      val links = ManagedCarCatalog.linkRuntimeInstances(cars, Vector(instance))

      When("the catalog derives its runtime summary")
      val summary = ManagedCarCatalog.runtimeSummary("textus-scraper", Vector(instance), links)

      Then("source availability is not turned into a stale CAR status")
      summary.state shouldBe ManagedCarRuntimeState.NotRunning
      summary.activeInstanceIds shouldBe Vector.empty
      summary.staleInstanceIds shouldBe Vector("stale-instance")
    }

    "keep merged catalog ordering deterministic for generated source identifiers" in {
      Given("arbitrary source identifiers for two artifact identities")
      val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaLowerStr.suchThat(_.nonEmpty))) { values =>
        val sources = values.zipWithIndex.map { case (value, index) =>
          _source(if (index % 2 == 0) "textus-a" else "textus-b", ManagedCarSourceKind.Development, value, None, None)
        }.toVector

        val cars = ManagedCarCatalog.mergeSources(sources)

        cars.map(_.artifactId) == cars.map(_.artifactId).sorted &&
          cars.forall(car => car.sources.map(_.sourceId) == car.sources.map(_.sourceId).sorted)
      }

      When("the catalog merges those source snapshots")
      val result = ScalaCheckTest.check(ScalaCheckTest.Parameters.default, property)

      Then("artifact and source ordering remain deterministic")
      result.passed shouldBe true
    }
  }

  private def _source(
    artifactid: String,
    sourcekind: ManagedCarSourceKind,
    sourceid: String,
    componentname: Option[String],
    privatelocator: Option[String]
  ): ManagedCarSource =
    ManagedCarSource(
      artifactid,
      sourcekind,
      sourceid,
      componentname,
      Vector("0.1.0"),
      ManagedCarRefreshState.Available,
      Instant.parse("2026-07-21T00:00:00Z"),
      None,
      privatelocator
    )
}
