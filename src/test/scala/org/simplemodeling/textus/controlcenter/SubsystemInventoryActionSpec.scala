/*
 *  version Jul. 28, 2026
 * @version Aug. 12, 2026
 */
package org.simplemodeling.textus.controlcenter

import cats.~>
import org.goldenport.Consequence
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.cncf.action.Action
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{Capability, DataStoreContext, EntityStoreContext, ExecutionContext, Principal, PrincipalId, RuntimeContext, SecurityContext}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.EntityStoreSpace
import org.goldenport.cncf.event.EventEngine
import org.goldenport.cncf.spi.supervisor.{Supervisor, SupervisorRequest, SupervisorResult, SupervisorSocket, SupervisorState}
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.unitofwork.{CommitRecorder, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.impl.TextusControlCenterLauncherRegistrationAuthenticationProvider
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Instant
import java.nio.file.{Files, Path, Paths}

final class SubsystemInventoryActionSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "SubsystemInventory Actions" should {
    "registration and inventory authority" which {
    "persist launcher reports and return only safe administrative projections" in {
      Given("an in-memory Textus Control Center component with launcher and operator principals")
      val fixture = _fixture()
      val component = _component()
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val startedat = Instant.parse("2026-07-18T00:00:00Z")

      When("a launcher registers a running server instance")
      val registrationresult = _execute(component, launchercontext, _registration_request("registerSubsystem", startedat))
      val registered = registrationresult
        .toOption
        .getOrElse(fail(s"registration failed: $registrationresult"))
        .asInstanceOf[OperationResponse.RecordResponse]
        .record

      Then("the launcher receives a safe projection without its principal")
      registered.getString("instanceId") shouldBe Some("textuscontrolcenteractionspec")
      registered.getString("status") shouldBe Some("starting")
      registered.getString("executionMode") shouldBe Some("development")
      registered.getString("developmentDirectory") shouldBe Some("/work/textus-control-center")
      registered.getAny("registrationPrincipalId") shouldBe empty

      When("the launcher repeats the same registration")
      val repeated = _execute(component, launchercontext, _registration_request("registerSubsystem", startedat))
        .toOption
        .getOrElse(fail("repeated registration failed"))
        .asInstanceOf[OperationResponse.RecordResponse]
        .record

      Then("the repeat is projected as the same instance state")
      repeated.getString("instanceId") shouldBe registered.getString("instanceId")

      When("an operator lists and loads the registered instance")
      val listresult = _execute(component, operatorcontext, Request.ofService("SubsystemInventory", "listSubsystems"))
      val listed = listresult
        .toOption
        .getOrElse(fail(s"list failed: $listresult"))
        .asInstanceOf[OperationResponse.RecordResponse]
        .record
      val loaded = _execute(
        component,
        operatorcontext,
        Request.ofService("SubsystemInventory", "getSubsystem", properties = List(Property("instanceId", "textuscontrolcenteractionspec", None)))
      ).toOption.getOrElse(fail("detail failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("both administrative reads expose the same safe instance")
      _records(listed).map(_.getString("instanceId")) shouldBe Vector(Some("textuscontrolcenteractionspec"))
      loaded.getString("instanceId") shouldBe registered.getString("instanceId")
      loaded.getString("executionMode") shouldBe Some("development")
      loaded.getString("developmentDirectory") shouldBe Some("/work/textus-control-center")
      loaded.getAny("registrationPrincipalId") shouldBe empty

      When("an authenticated standalone operator capability reads the inventory")
      val localoperatorresult = _execute(component, fixture.operatorCapabilityContext, Request.ofService("SubsystemInventory", "listSubsystems"))

      Then("the local operator capability is accepted without launcher authority")
      localoperatorresult.toOption should not be empty

      When("the launcher sends a heartbeat and normal termination")
      val heartbeatresult = _execute(component, launchercontext, _registration_request("heartbeatSubsystem", startedat))
      val heartbeated = heartbeatresult
        .toOption
        .getOrElse(fail(s"heartbeat failed: $heartbeatresult"))
        .asInstanceOf[OperationResponse.RecordResponse]
        .record
      val stopped = _execute(
        component,
        launchercontext,
        Request.ofService("SubsystemInventory", "deregisterSubsystem", properties = List(Property("instanceId", "textuscontrolcenteractionspec", None)))
      ).toOption.getOrElse(fail("deregister failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("the instance is retained as stopped")
      heartbeated.getString("status") shouldBe Some("running")
      stopped.getString("status") shouldBe Some("stopped")
    }

    "reject registration with an invalid subsystem base URL" in {
      Given("an authenticated launcher registration containing a malformed base URL")
      val fixture = _fixture()
      val component = _component()
      val context = fixture.launcherContextFor(SecurityContext.Privilege.Internal)
      val startedat = Instant.parse("2026-07-18T00:00:00Z")

      When("the launcher registers the subsystem")
      val result = _execute(
        component,
        context,
        _registration_request(
          "registerSubsystem",
          startedat,
          baseurl = "not-a-subsystem-url"
        )
      )

      Then("registration fails with the base URL validation evidence")
      result.toOption shouldBe empty
      result.toString should include ("baseUrl must be an absolute HTTP URL")
    }

    "list a registration with omitted optional metadata" in {
      Given("a launcher registration that supplies only the required protocol fields")
      val fixture = _fixture()
      val component = _component()
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val startedat = Instant.parse("2026-07-28T00:00:00Z")

      When("the launcher registers and an operator lists the instance")
      _execute(component, launchercontext, _registration_request("registerSubsystem", startedat, includeoptional = false)).toOption should not be empty
      val listed = _execute(component, operatorcontext, Request.ofService("SubsystemInventory", "listSubsystems"))
        .toOption.getOrElse(fail("list with omitted optional metadata failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("the safe projection preserves the instance and represents omitted metadata without a serialization failure")
      _records(listed).map(_.getString("instanceId")) shouldBe Vector(Some("textuscontrolcenteractionspec"))
      _records(listed).head.getAny("artifactId") shouldBe empty
      _records(listed).head.getAny("subsystemVersion") shouldBe empty
    }

    "reject administrative reads from a non-operator principal" in {
      Given("a Textus Control Center component and a launcher-level principal")
      val fixture = _fixture()
      val component = _component()
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.User)

      When("the launcher attempts to list the inventory")
      val result = _execute(component, launchercontext, Request.ofService("SubsystemInventory", "listSubsystems"))

      Then("the operation returns a structured authorization failure")
      result.toOption shouldBe empty
    }

    "reject registration from an authenticated principal without launcher capability" in {
      Given("a Textus Control Center component and a human-level authenticated principal")
      val fixture = _fixture()
      val component = _component()
      val context = fixture.contextFor(SecurityContext.Privilege.User)
      val startedat = Instant.parse("2026-07-18T00:00:00Z")

      When("the principal attempts a launcher registration")
      val result = _execute(component, context, _registration_request("registerSubsystem", startedat))

      Then("the operation requires the launcher-registration capability")
      result.toOption shouldBe empty
      result.toString should include ("authenticated launcher principal")
    }

    "reject a heartbeat from a different authenticated launcher principal" in {
      Given("a launcher-owned registered instance")
      val fixture = _fixture()
      val component = _component()
      val ownercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)
      val othercontext = fixture.launcherContextFor(SecurityContext.Privilege.User)
      val startedat = Instant.parse("2026-07-18T00:00:00Z")
      _execute(component, ownercontext, _registration_request("registerSubsystem", startedat)).toOption should not be empty

      When("another authenticated launcher reports a heartbeat")
      val result = _execute(component, othercontext, _registration_request("heartbeatSubsystem", startedat))

      Then("the operation rejects the foreign owner")
      result.toOption shouldBe empty
      result.toString should include ("not authorized")
    }

    "retain a standalone registry across a local datastore restart" in {
      Given("a standalone Control Center application datastore")
      val datastorepath = _local_datastore_path("control-center-standalone-restart")
      val configuration = _standalone_configuration(datastorepath)
      val firstfixture = _fixture(Some(datastorepath))
      val firstcomponent = _component(configuration)
      val startedat = Instant.parse("2026-07-19T00:00:00Z")

      When("a launcher registers an instance before the standalone runtime stops")
      val registrationresult = _execute(
        firstcomponent,
        firstfixture.launcherContextFor(SecurityContext.Privilege.Internal),
        _registration_request("registerSubsystem", startedat)
      )
      registrationresult.toOption.getOrElse(fail(s"standalone registration failed: $registrationresult"))

      And("the owning first component subsystem shuts down before the restart")
      val shutdownresult = firstcomponent.subsystem
        .getOrElse(fail("first Control Center component has no owning subsystem"))
        .shutdownC()

      And("a newly constructed runtime opens the same local datastore through managed binding")
      val secondfixture = _fixture(Some(datastorepath))
      val secondcomponent = _component(configuration)
      val result = _execute(
        secondcomponent,
        secondfixture.contextFor(SecurityContext.Privilege.ApplicationContentManager),
        Request.ofService("SubsystemInventory", "listSubsystems")
      ).toOption.getOrElse(fail("standalone registry was not available after restart"))
        .asInstanceOf[OperationResponse.RecordResponse]
        .record

      Then("the safe inventory projection retains the accepted instance")
      shutdownresult.toOption should not be empty
      _records(result).map(_.getString("instanceId")) shouldBe Vector(Some("textuscontrolcenteractionspec"))
      _records(result).flatMap(_.getAny("registrationPrincipalId")) shouldBe empty
    }

    }

    "catalog and operational target management" which {
    "refresh, list, and load managed CAR snapshots through the protected operation surface" in {
      Given("a configured standalone development root with one CAR descriptor")
      val root = Files.createTempDirectory("control-center-catalog")
      val car = Files.createDirectory(root.resolve("textus-catalog-spec"))
      Files.writeString(
        car.resolve("project.yaml"),
        "project:\n  namespace: org.simplemodeling.textus\n  id: CatalogSpec\n  kind: car\n  component:\n    name: catalog-spec-component\n"
      )
      val fixture = _fixture()
      val component = _component(_catalog_configuration(root))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)

      When("a launcher reports an artifact-identified running instance and an operator reads the catalog")
      _execute(component, launchercontext, _registration_request("registerSubsystem", Instant.parse("2026-07-21T00:00:00Z"), Some("textus-catalog-spec"))).toOption should not be empty
      _execute(component, launchercontext, _registration_request("heartbeatSubsystem", Instant.parse("2026-07-21T00:00:00Z"), Some("textus-catalog-spec"))).toOption should not be empty
      val refreshresult = _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog"))
      val refresh = refreshresult
        .toOption.getOrElse(fail(s"catalog refresh failed: $refreshresult"))
        .asInstanceOf[OperationResponse.RecordResponse].record
      val listresult = _execute(component, operatorcontext, Request.ofService("CarCatalog", "listManagedCars"))
      val listed = listresult
        .toOption.getOrElse(fail(s"catalog list failed: $listresult"))
        .asInstanceOf[OperationResponse.RecordResponse].record
      val detail = _execute(
        component,
        operatorcontext,
        Request.ofService("CarCatalog", "getManagedCar", properties = List(Property("artifactId", "textus-catalog-spec", None)))
      ).toOption.getOrElse(fail("catalog detail failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("the shared operations persist a deterministic catalog while redacting locators from the list")
      refresh.getInt("refreshedSourceCount") shouldBe Some(1)
      _records(listed).map(_.getString("artifactId")) shouldBe Vector(Some("textus-catalog-spec"))
      _records(listed).head.getString("runtimeState") shouldBe Some("running")
      _records(listed).head.getAny("activeInstanceIds") should not be empty
      val listsource = _records(listed).head.getAny("sources") match {
        case Some(values: Vector[?]) => values.collectFirst { case value: Record => value }.getOrElse(fail("list source missing"))
        case Some(values: Seq[?]) => values.collectFirst { case value: Record => value }.getOrElse(fail("list source missing"))
        case _ => fail("list source missing")
      }
      listsource.getString("sourceKind") shouldBe Some("DEV")
      listsource.getAny("privateLocator") shouldBe empty
      val detailsource = detail.getAny("sources") match {
        case Some(values: Vector[?]) => values.collectFirst { case value: Record => value }.getOrElse(fail("detail source missing"))
        case Some(values: Seq[?]) => values.collectFirst { case value: Record => value }.getOrElse(fail("detail source missing"))
        case _ => fail("detail source missing")
      }
      detailsource.getString("privateLocator") shouldBe Some(car.toString)

      When("a launcher-level principal attempts a catalog read")
      val unauthorized = _execute(component, fixture.launcherContextFor(SecurityContext.Privilege.User), Request.ofService("CarCatalog", "listManagedCars"))

      Then("the catalog boundary rejects non-administrative authority")
      unauthorized.toOption shouldBe empty
    }

    "read persisted catalog snapshots without observing a live development endpoint" in {
      Given("a persisted development CAR whose matching assembly endpoint is live")
      val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
      server.createContext("/rest/v1/admin/assembly/descriptor", exchange => {
        val body = """{"subsystem":"textus-catalog-read-spec","version":"0.1.0-SNAPSHOT"}""".getBytes
        exchange.sendResponseHeaders(200, body.length)
        val output = exchange.getResponseBody
        try output.write(body)
        finally output.close()
      })
      server.start()
      try {
        val root = Files.createTempDirectory("control-center-catalog-read")
        val car = Files.createDirectory(root.resolve("textus-catalog-read-spec"))
        Files.writeString(
          car.resolve("project.yaml"),
          s"project:\n  namespace: org.simplemodeling.textus\n  id: CatalogReadSpec\n  kind: car\n  component:\n    version: 0.1.0-SNAPSHOT\n    config:\n      textus.server.default-port: \"${server.getAddress.getPort}\"\n"
        )
        val fixture = _fixture()
        val component = _component(_catalog_configuration(root))
        val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)

        When("the operator refreshes once and later lists and loads the persisted CAR")
        _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
        val listed = _execute(component, operatorcontext, Request.ofService("CarCatalog", "listManagedCars"))
          .toOption.getOrElse(fail("catalog list failed"))
          .asInstanceOf[OperationResponse.RecordResponse].record
        val detail = _execute(
          component,
          operatorcontext,
          Request.ofService("CarCatalog", "getManagedCar", properties = List(Property("artifactId", "textus-catalog-read-spec", None)))
        ).toOption.getOrElse(fail("catalog detail failed")).asInstanceOf[OperationResponse.RecordResponse].record

        Then("both projections retain only registered runtime evidence and make no live observation")
        _records(listed).head.getString("runtimeState") shouldBe Some("not-running")
        _records(listed).head.getAny("observedBaseUrls") shouldBe empty
        detail.getString("runtimeState") shouldBe Some("not-running")
        detail.getAny("observedBaseUrls") shouldBe empty
      } finally {
        server.stop(0)
      }
    }

    "refresh every configured standalone development root" in {
      Given("a standalone catalog file with two explicitly declared development roots")
      val firstroot = Files.createTempDirectory("control-center-catalog-first")
      val secondroot = Files.createTempDirectory("control-center-catalog-second")
      _write_car_descriptor(firstroot, "textus-catalog-first", "catalog-first-component")
      _write_car_descriptor(secondroot, "textus-catalog-second", "catalog-second-component")
      val catalogfile = Files.createTempFile("control-center-catalog", ".yaml")
      Files.writeString(
        catalogfile,
        s"""schema: textus-control-center.catalog.v1
           |development:
           |  roots:
           |    - id: first
           |      path: ${firstroot.toString}
           |      include-prefix: textus-
           |    - id: second
           |      path: ${secondroot.toString}
           |      include-prefix: textus-
           |""".stripMargin
      )
      val fixture = _fixture()
      val component = _component(_catalog_file_configuration(catalogfile))

      When("an operator refreshes the configured catalog")
      val refreshed = _execute(
        component,
        fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager),
        Request.ofService("CarCatalog", "refreshCarCatalog")
      ).toOption.getOrElse(fail("catalog refresh failed")).asInstanceOf[OperationResponse.RecordResponse].record
      val listed = _execute(
        component,
        fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager),
        Request.ofService("CarCatalog", "listManagedCars")
      ).toOption.getOrElse(fail("catalog list failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("both declared roots contribute their direct CAR projects")
      refreshed.getInt("refreshedSourceCount") shouldBe Some(2)
      _records(listed).map(_.getString("artifactId")) shouldBe Vector(Some("textus-catalog-first"), Some("textus-catalog-second"))
    }

    "manage development CARs as operational targets while retaining an explicit exclusion" in {
      Given("a configured standalone development root with one CAR descriptor")
      val root = Files.createTempDirectory("control-center-operational-management")
      _write_car_descriptor(root, "textus-operational-spec", "operational-spec-component")
      val fixture = _fixture()
      val component = _component(_catalog_configuration(root))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)

      When("the operator refreshes the catalog")
      _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val listed = _execute(component, operatorcontext, Request.ofService("OperationalManagement", "listOperationalComponents"))
        .toOption.getOrElse(fail("operational component list failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("the development CAR is automatically retained as an operating target")
      _records(listed).map(_.getString("artifactId")) shouldBe Vector(Some("textus-operational-spec"))
      _records(listed).head.getString("managementState") shouldBe Some("auto-managed")

      When("a launcher reports accepted use of the development CAR")
      val registrationresult = _execute(
        component,
        launchercontext,
        _registration_request("registerSubsystem", Instant.parse("2026-07-22T00:00:00Z"), Some("textus-operational-spec"))
      )
      registrationresult.toOption.getOrElse(fail(s"development registration failed: $registrationresult"))
      val afterregistration = _execute(component, operatorcontext, Request.ofService("OperationalManagement", "getOperationalComponent", properties = List(Property("artifactId", "textus-operational-spec", None))))
        .toOption.getOrElse(fail("operational component detail failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("development auto-management remains the stronger management reason")
      afterregistration.getString("managementState") shouldBe Some("auto-managed")

      When("the operator removes the target from operational management and refreshes again")
      val removalresult = _execute(
        component,
        operatorcontext,
        Request.ofService("OperationalManagement", "removeOperationalComponent", properties = List(Property("artifactId", "textus-operational-spec", None)))
      )
      val removed = removalresult.toOption.getOrElse(fail(s"operational component removal failed: $removalresult")).asInstanceOf[OperationResponse.RecordResponse].record
      _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val excluded = _execute(component, operatorcontext, Request.ofService("OperationalManagement", "listOperationalComponents"))
        .toOption.getOrElse(fail("excluded operational component list failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("the explicit exclusion persists and hides the target from the panel list")
      removed.getString("managementState") shouldBe Some("excluded")
      _records(excluded) shouldBe empty

      When("the operator restores the excluded target")
      val restored = _execute(
        component,
        operatorcontext,
        Request.ofService("OperationalManagement", "restoreOperationalComponent", properties = List(Property("artifactId", "textus-operational-spec", None)))
      ).toOption.getOrElse(fail("operational component restoration failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("the development evidence restores its auto-managed state")
      restored.getString("managementState") shouldBe Some("auto-managed")
    }

    "adopt a component after accepted launcher use without a development source" in {
      Given("a Control Center receiving an artifact-identified launcher registration")
      val fixture = _fixture()
      val component = _component()
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)

      When("the launcher registers the component instance")
      _execute(
        component,
        launchercontext,
        _registration_request("registerSubsystem", Instant.parse("2026-07-22T00:00:00Z"), Some("textus-adopted-spec"))
      ).toOption should not be empty
      val listed = _execute(component, operatorcontext, Request.ofService("OperationalManagement", "listOperationalComponents"))
        .toOption.getOrElse(fail("adopted operational component list failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("accepted launcher use retains the component as adopted")
      _records(listed).map(_.getString("artifactId")) shouldBe Vector(Some("textus-adopted-spec"))
      _records(listed).head.getString("managementState") shouldBe Some("adopted")
    }

    }

    "launcher evidence reconciliation" which {
    "reconcile Launcher evidence retained before Control Center startup without direct shared-file access" in {
      Given("a bounded CNCF Launcher command whose evidence was retained before Control Center startup")
      val root = Files.createTempDirectory("control-center-launcher-evidence")
      val externalcommand = sys.env.get("TEXTUS_CONTROL_CENTER_PHASE4_EVIDENCE_COMMAND").map(java.nio.file.Path.of(_))
      val command = externalcommand.getOrElse(root.resolve("cncf-evidence"))
      if (externalcommand.isEmpty)
        Files.writeString(command,
          """#!/bin/sh
          |if [ "$3" = "list" ]; then
          |  echo '{"schema":"cncf.launcher.evidence-projection.v1","entries":[{"launcherKind":"cncf","instanceId":"cncf-current-instance","target":"cncf-evidence-spec","artifactId":"cncf-evidence-spec","executionMode":"development","subsystemName":"CNCF Evidence Spec","subsystemVersion":"0.1.0-SNAPSHOT","runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":null},{"launcherKind":"textus","instanceId":"textus-stopped-instance","target":"textus-evidence-spec","artifactId":"textus-evidence-spec","executionMode":"artifact","subsystemName":"Textus Evidence Spec","subsystemVersion":"0.1.0-SNAPSHOT","runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":"2026-07-22T00:01:00Z"}]}'
          |elif [ "$4" = "cncf-current-instance" ]; then
          |  echo '{"schema":"cncf.launcher.evidence-projection.v1","entry":{"launcherKind":"cncf","instanceId":"cncf-current-instance","target":"cncf-evidence-spec","artifactId":"cncf-evidence-spec","executionMode":"development","developmentDirectory":"/private/work/cncf-evidence-spec","subsystemName":"CNCF Evidence Spec","subsystemVersion":"0.1.0-SNAPSHOT","runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":null}}'
          |else
          |  echo '{"schema":"cncf.launcher.evidence-projection.v1","entry":{"launcherKind":"textus","instanceId":"textus-stopped-instance","target":"textus-evidence-spec","artifactId":"textus-evidence-spec","executionMode":"artifact","developmentDirectory":null,"subsystemName":"Textus Evidence Spec","subsystemVersion":"0.1.0-SNAPSHOT","runtimeVersion":"0.5.0-SNAPSHOT","startedAt":"2026-07-22T00:00:00Z","lastSeenAt":"2026-07-22T00:00:30Z","stoppedAt":"2026-07-22T00:01:00Z"}}'
          |fi
          |""".stripMargin
        )
      if (externalcommand.isEmpty)
        command.toFile.setExecutable(true) shouldBe true
      val fixture = _fixture()
      val component = _component(_launcher_evidence_configuration(command, externalcommand.map(_ => "30s")))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)

      When("the later-started Control Center refreshes, lists, and loads the evidence Operations")
      val refreshresult = _execute(component, operatorcontext, Request.ofService("LauncherEvidence", "refreshLauncherEvidence"))
      val refreshed = refreshresult.toOption.getOrElse(fail(s"evidence refresh failed: $refreshresult")).asInstanceOf[OperationResponse.RecordResponse].record
      _execute(component, operatorcontext, Request.ofService("LauncherEvidence", "refreshLauncherEvidence")).toOption should not be empty
      val listed = _execute(component, operatorcontext, Request.ofService("LauncherEvidence", "listLauncherEvidence"))
        .toOption.getOrElse(fail("evidence list failed")).asInstanceOf[OperationResponse.RecordResponse].record
      val detailid = externalcommand.fold("cncf-current-instance") { _ =>
        _records(listed).find(_.getString("launcherKind").contains("cncf")).flatMap(_.getString("instanceId")).getOrElse(fail("external CNCF evidence was not retained"))
      }
      val detail = _execute(component, operatorcontext, Request.ofService("LauncherEvidence", "getLauncherEvidence", properties = List(Property("instanceId", detailid, None))))
        .toOption.getOrElse(fail("evidence detail failed")).asInstanceOf[OperationResponse.RecordResponse].record
      val operating = _execute(component, operatorcontext, Request.ofService("OperationalManagement", "listOperationalComponents"))
        .toOption.getOrElse(fail("operating target list failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("safe rows preserve both launcher kinds and derive their retained decisions without direct shared-file access")
      _records(listed).map(_.getString("launcherKind")) should contain allOf (Some("cncf"), Some("textus"))
      _records(listed).forall(_.getAny("developmentDirectory").isEmpty) shouldBe true
      externalcommand match {
        case Some(_) =>
          refreshed.getInt("totalCount") shouldBe Some(2)
          _records(listed) should have size 2
          _records(listed).map(_.getString("evidenceDecision")) should contain allOf (Some("current-evidence-only"), Some("historical-stopped"))
          detail.getString("developmentDirectory").map(_.nonEmpty) shouldBe Some(true)
          _records(operating).map(_.getString("artifactId")) should contain (Some("textus-control-center"))
        case None =>
          refreshed.getInt("totalCount") shouldBe Some(2)
          _records(listed) should have size 2
          _records(listed).map(_.getString("evidenceDecision")) should contain allOf (Some("current-evidence-only"), Some("historical-stopped"))
          detail.getString("developmentDirectory") shouldBe Some("/private/work/cncf-evidence-spec")
          _records(operating).map(_.getString("artifactId")) should contain (Some("cncf-evidence-spec"))
          _records(operating).map(_.getString("artifactId")) should not contain Some("textus-evidence-spec")
      }
    }

    }

    "lifecycle control" which {
    "retain an idempotent lifecycle request without a launcher-private lifecycle command" in {
      Given("an auto-managed development component with a legacy launcher command that must be ignored")
      val root = Files.createTempDirectory("control-center-lifecycle-request")
      _write_car_descriptor(root, "textus-lifecycle-spec", "lifecycle-spec-component")
      val fixture = _fixture()
      val component = _component(_lifecycle_configuration(root, Map(
        "textus-control-center.launcher.lifecycle.command" -> "/usr/bin/false"
      )))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val request = Request.ofService(
        "LifecycleControl",
        "startOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-spec", None),
          Property("idempotencyKey", "lifecycle-request-spec-key", None),
          Property("sourceKind", "DEV", None),
          Property("sourceId", "standalone-development", None)
        )
      )

      When("the operator requests a start and retries with the same idempotency key")
      val first = _execute(component, operatorcontext, request)
        .toOption.getOrElse(fail("lifecycle request failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record
      val second = _execute(component, operatorcontext, request)
        .toOption.getOrElse(fail("lifecycle request retry failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record
      val audit = _execute(
        component,
        operatorcontext,
        Request.ofService("LifecycleControl", "listLifecycleRequests", properties = List(Property("artifactId", "textus-lifecycle-spec", None)))
      ).toOption.getOrElse(fail("lifecycle request audit list failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("the embedded supervisor request is queued and does not create a duplicate request")
      first.getString("requestState") shouldBe Some("queued")
      first.getAny("deadlineAt") should not be empty
      first.getAny("acceptedAt") shouldBe empty
      first.getString("launchProfileId") shouldBe Some("DEV:standalone-development")
      first.getAny("diagnosticCode") shouldBe empty
      first.getAny("diagnostic") shouldBe empty
      second.getString("requestId") shouldBe first.getString("requestId")
      _records(audit).map(_.getString("requestId")) shouldBe Vector(first.getString("requestId"))
      _records(audit).head.getAny("idempotencyKey") shouldBe empty
      _records(audit).head.getAny("operatorSubjectId") shouldBe empty
    }

    "stop without a source while restart retains an exact required source selector" in {
      Given("an auto-managed development component and an embedded supervisor home")
      val root = Files.createTempDirectory("control-center-lifecycle-source-selector")
      _write_car_descriptor(root, "textus-lifecycle-source-selector-spec", "lifecycle-source-selector-component")
      val fixture = _fixture()
      val component = _component(_lifecycle_configuration(root, Map.empty))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val stoprequest = Request.ofService(
        "LifecycleControl",
        "stopOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-source-selector-spec", None),
          Property("idempotencyKey", "lifecycle-stop-source-selector-spec-key", None)
        )
      )
      val missingrestartsource = Request.ofService(
        "LifecycleControl",
        "restartOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-source-selector-spec", None),
          Property("idempotencyKey", "lifecycle-restart-source-selector-missing-key", None)
        )
      )
      val restartrequest = Request.ofService(
        "LifecycleControl",
        "restartOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-source-selector-spec", None),
          Property("idempotencyKey", "lifecycle-restart-source-selector-spec-key", None),
          Property("sourceKind", "DEV", None),
          Property("sourceId", "standalone-development", None)
        )
      )

      When("the operator stops without a source and restarts with and without the exact source")
      val stopped = _execute(component, operatorcontext, stoprequest)
        .toOption.getOrElse(fail("source-free stop request failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record
      val missing = _execute(component, operatorcontext, missingrestartsource)
      val restarted = _execute(component, operatorcontext, restartrequest)
        .toOption.getOrElse(fail("source-selected restart request failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("stop is artifact-only and restart retains only the required exact source")
      stopped.getString("requestState") shouldBe Some("queued")
      stopped.getAny("launchProfileId") shouldBe empty
      missing.toOption shouldBe empty
      restarted.getString("requestState") shouldBe Some("queued")
      restarted.getString("launchProfileId") shouldBe Some("DEV:standalone-development")
    }

    "reject a restart when its current source snapshot became unavailable" in {
      Given("an adopted component with an initially available local CAR archive")
      val repository = Files.createTempDirectory("control-center-lifecycle-source-transition")
      val catalogroot = Files.createDirectories(repository.resolve("repository/catalog/car"))
      val archive = repository.resolve("repository/car/textus-lifecycle-source-transition-spec/0.1.0/textus-lifecycle-source-transition-spec-0.1.0.car")
      Files.createDirectories(archive.getParent)
      Files.writeString(archive, "CAR")
      Files.writeString(
        catalogroot.resolve("textus-lifecycle-source-transition-spec.yaml"),
        "kind: car\nartifactId: textus-lifecycle-source-transition-spec\nversions:\n  - version: 0.1.0\n    file: repository/car/textus-lifecycle-source-transition-spec/0.1.0/textus-lifecycle-source-transition-spec-0.1.0.car\n"
      )
      val catalogfile = Files.createTempFile("control-center-lifecycle-source-transition", ".yaml")
      Files.writeString(
        catalogfile,
        s"schema: textus-control-center.catalog.v1\nlocal-repository:\n  id: local-transition\n  catalog-root: ${catalogroot.toString}\n"
      )
      val fixture = _fixture()
      val component = _component(_catalog_file_lifecycle_configuration(catalogfile, repository))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)
      _execute(component, launchercontext, _registration_request("registerSubsystem", Instant.parse("2026-08-10T00:00:00Z"), Some("textus-lifecycle-source-transition-spec"))).toOption should not be empty
      _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val availablerestart = Request.ofService(
        "LifecycleControl",
        "restartOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-source-transition-spec", None),
          Property("idempotencyKey", "lifecycle-source-transition-available-key", None),
          Property("sourceKind", "LOCAL", None),
          Property("sourceId", "local-transition", None)
        )
      )
      val unavailablerestart = Request.ofService(
        "LifecycleControl",
        "restartOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-source-transition-spec", None),
          Property("idempotencyKey", "lifecycle-source-transition-unavailable-key", None),
          Property("sourceKind", "LOCAL", None),
          Property("sourceId", "local-transition", None)
        )
      )

      When("the archive disappears after an available refresh and the catalog records a newer unavailable source snapshot")
      val available = _execute(component, operatorcontext, availablerestart)
        .toOption.getOrElse(fail("available local source restart failed"))
        .asInstanceOf[OperationResponse.RecordResponse].record
      Files.delete(archive)
      _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val unavailable = _execute(component, operatorcontext, unavailablerestart)
        .toOption.getOrElse(fail("unavailable local source restart did not return a request"))
        .asInstanceOf[OperationResponse.RecordResponse].record

      Then("restart accepts the available snapshot but never falls back after its newer unavailable transition")
      available.getString("requestState") shouldBe Some("queued")
      unavailable.getString("requestState") shouldBe Some("rejected")
      unavailable.getString("diagnosticCode") shouldBe Some("supervisor-launch-profile-unavailable")
    }

    "require an embedded supervisor home while ignoring legacy Launcher lifecycle declarations" in {
      Given("an auto-managed development component with and without an embedded supervisor home")
      val root = Files.createTempDirectory("control-center-lifecycle-supervisor")
      _write_car_descriptor(root, "textus-lifecycle-supervisor-spec", "lifecycle-supervisor-spec-component")
      val fixture = _fixture()
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val incompletecomponent = _component(_catalog_configuration(root))
      val configuredcomponent = _component(_lifecycle_configuration(root, Map(
        "textus-control-center.launcher.lifecycle.command" -> "invalid legacy command"
      )))
      val incompleterequest = Request.ofService(
        "LifecycleControl",
        "startOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-supervisor-spec", None),
          Property("idempotencyKey", "lifecycle-incomplete-supervisor-spec-key", None),
          Property("sourceKind", "DEV", None),
          Property("sourceId", "standalone-development", None)
        )
      )
      val configuredrequest = Request.ofService(
        "LifecycleControl",
        "startOperationalComponent",
        properties = List(
          Property("artifactId", "textus-lifecycle-supervisor-spec", None),
          Property("idempotencyKey", "lifecycle-configured-supervisor-spec-key", None),
          Property("sourceKind", "DEV", None),
          Property("sourceId", "standalone-development", None)
        )
      )

      When("the operator creates lifecycle requests")
      _execute(incompletecomponent, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      _execute(configuredcomponent, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog")).toOption should not be empty
      val incomplete = _execute(incompletecomponent, operatorcontext, incompleterequest).toOption.getOrElse(fail("incomplete supervisor request failed")).asInstanceOf[OperationResponse.RecordResponse].record
      val configured = _execute(configuredcomponent, operatorcontext, configuredrequest).toOption.getOrElse(fail("configured supervisor request failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("only the missing embedded supervisor home rejects the request")
      incomplete.getString("requestState") shouldBe Some("rejected")
      incomplete.getString("diagnosticCode") shouldBe Some("textus-supervisor-home-unavailable")
      incomplete.getAny("supervisorId") shouldBe empty
      configured.getString("requestState") shouldBe Some("queued")
      configured.getAny("diagnosticCode") shouldBe empty
      configured.getAny("supervisorId") shouldBe empty
    }

    }
  }

  private def _fixture(datastorepath: Option[Path] = None): Fixture = {
    val datastorespace = datastorepath match {
      case Some(_) => new DataStoreSpace()
      case None => new DataStoreSpace().addDataStore(DataStore.inMemorySearchable())
    }
    val entitystorespace = EntityStoreSpace.create(
      ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
    )
    val base = ExecutionContext.create()
    val core = RuntimeContext.core(
      name = "textus-control-center-action-spec",
      parent = None,
      observabilityContext = base.observability,
      datastore = Some(DataStoreContext(datastorespace)),
      entitystore = Some(EntityStoreContext(entitystorespace))
    )
    val eventengine = EventEngine.noop(DataStore.noop())
    def _build_(privilege: SecurityContext.Privilege, extracapabilities: Set[Capability]): ExecutionContext = {
      lazy val context: ExecutionContext = ExecutionContext.withSecurityContext(
        ExecutionContext.withRuntimeContext(base, runtime),
        SecurityContext(
          principal = new Principal {
            def id: PrincipalId = privilege.principalId
            def attributes: Map[String, String] =
              privilege.attributes + ("access_token" -> s"token-${privilege.principalId.value}")
          },
          capabilities = privilege.capabilities ++ extracapabilities,
          level = privilege.level,
          subjectKind = privilege.subjectKind
        )
      )
      lazy val uow = new UnitOfWork(context, eventengine, CommitRecorder.noop)
      lazy val interpreter = new UnitOfWorkInterpreter(uow)
      lazy val unitofworkinterpreter = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] =
          Consequence(interpreter.execute(operation))
      }
      lazy val runtime = new RuntimeContext(
        core = core,
        unitOfWorkSupplier = () => uow,
        unitOfWorkInterpreterFn = unitofworkinterpreter,
        commitAction = unitofwork => { val _ = unitofwork.commit(); () },
        abortAction = unitofwork => { val _ = unitofwork.rollback(); () },
        disposeAction = _ => (),
        token = "textus-control-center-action-spec"
      )
      context
    }
    Fixture(_build_)
  }

  private def _component(configuration: ResolvedConfiguration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)): Component = {
    val subsystem = Subsystem(
      name = "textus-control-center-action-spec",
      configuration = configuration
    )
    val bundle = new impl.ComponentFactory().create(ComponentCreate(subsystem, ComponentOrigin.Main))
    val component = new org.goldenport.cncf.component.ComponentFactory().bootstrap(bundle.participants.head)
    component match {
      case socket: SupervisorSocket => socket.withSupervisor(_test_supervisor)
      case _ => fail("Textus Control Center must expose the Supervisor SPI socket")
    }
    subsystem.add(component)
    component
  }

  private val _test_supervisor: Supervisor = new Supervisor {
    def submit(request: SupervisorRequest)(using ExecutionContext): Consequence[SupervisorResult] =
      Consequence.success(SupervisorResult(
        request.requestId,
        SupervisorState.Accepted,
        None,
        None,
        "textus-supervisor-test",
        Some(s"instance-${request.requestId}"),
        Some(summon[ExecutionContext].clock.instant()),
        None
      ))

    def lookup(requestId: String)(using ExecutionContext): Consequence[Option[SupervisorResult]] =
      Consequence.success(None)
  }

  private def _execute(
    component: Component,
    context: ExecutionContext,
    request: Request
  ): Consequence[OperationResponse] =
    component.logic.makeOperationRequest(request).flatMap {
      case action: Action => component.actionEngine.execute(component.logic.createActionCall(action, context))
      case other => Consequence.operationInvalid("request", s"request did not resolve to action: $other")
    }

  private def _registration_request(
    operation: String,
    startedat: Instant,
    artifactid: Option[String] = None,
    baseurl: String = "http://127.0.0.1:8080",
    includeoptional: Boolean = true
  ): Request =
    Request.ofService(
      "SubsystemInventory",
      operation,
      properties = List(
        Property("protocolVersion", 1, None),
        Property("instanceId", "textuscontrolcenteractionspec", None),
        Property("launcherKind", "textus", None),
        Property("target", "textus-control-center", None),
        Property("baseUrl", baseurl, None),
        Property("hostLabel", "actionspec", None),
        Property("startedAt", startedat, None),
        Property("launcherState", if (operation == "registerSubsystem") "starting" else "running", None)
      ) ++
        (if (includeoptional)
          List(
            Property("executionMode", "development", None),
            Property("developmentDirectory", "/work/textus-control-center", None),
            Property("subsystemName", "TextusControlCenter", None),
            Property("subsystemVersion", "v010snapshot", None),
            Property("runtimeVersion", "v050", None)
          )
        else Nil) ++
        artifactid.map(value => Property("artifactId", value, None)).toList
    )

  private def _records(record: Record): Vector[Record] =
    record.getAny("data") match {
      case Some(values: Vector[?]) => values.collect { case value: Record => value }
      case Some(values: Seq[?]) => values.collect { case value: Record => value }.toVector
      case Some(value: Record) => Vector(value)
      case _ => Vector.empty
    }

  private def _local_datastore_path(prefix: String): Path = {
    val directory = Paths.get("target", "test-tmp", "textus-control-center")
    Files.createDirectories(directory)
    Files.createTempFile(directory, prefix, ".db")
  }

  private def _standalone_configuration(path: Path): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(
        "textus.component.org.simplemodeling.textus.control-center.datastores.application.policy" -> ConfigurationValue.StringValue("local-default"),
        "textus.local-data.org.simplemodeling.textus.control-center.application.path" -> ConfigurationValue.StringValue(path.toString)
      )),
      ConfigurationTrace.empty
    )

  private def _catalog_configuration(root: Path): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(
        "textus-control-center.catalog.development.root" -> ConfigurationValue.StringValue(root.toString)
      )),
      ConfigurationTrace.empty
    )

  private def _launcher_evidence_configuration(command: Path, timeout: Option[String] = None): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(
        Map("textus-control-center.launcher.evidence.command" -> ConfigurationValue.StringValue(command.toString)) ++
          timeout.map(value => "textus-control-center.launcher.evidence.timeout" -> ConfigurationValue.StringValue(value))
      ),
      ConfigurationTrace.empty
    )

  private def _lifecycle_configuration(root: Path, values: Map[String, String]): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(
        Map(
          "textus-control-center.catalog.development.root" -> ConfigurationValue.StringValue(root.toString),
          "textus-control-center.home" -> ConfigurationValue.StringValue(root.resolve(".control-center").toString)
        ) ++
          values.map { case (key, value) => key -> ConfigurationValue.StringValue(value) }
      ),
      ConfigurationTrace.empty
    )

  private def _catalog_file_configuration(path: Path): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(
        "textus-control-center.catalog.file" -> ConfigurationValue.StringValue(path.toString)
      )),
      ConfigurationTrace.empty
    )

  private def _catalog_file_lifecycle_configuration(catalogfile: Path, home: Path): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(
        "textus-control-center.catalog.file" -> ConfigurationValue.StringValue(catalogfile.toString),
        "textus-control-center.home" -> ConfigurationValue.StringValue(home.resolve(".control-center").toString)
      )),
      ConfigurationTrace.empty
    )

  private def _write_car_descriptor(root: Path, artifactid: String, componentname: String): Unit = {
    val project = Files.createDirectory(root.resolve(artifactid))
    val componentid = artifactid.stripPrefix("textus-").split("-").iterator.map(_.capitalize).mkString
    Files.writeString(
      project.resolve("project.yaml"),
      s"project:\n  namespace: org.simplemodeling.textus\n  id: $componentid\n  kind: car\n  component:\n    name: $componentname\n    config:\n      textus.server.default-port: \"19000\"\n"
    )
  }

  private final case class Fixture(
    build: (SecurityContext.Privilege, Set[Capability]) => ExecutionContext
  ) {
    def contextFor(privilege: SecurityContext.Privilege): ExecutionContext =
      build(privilege, Set.empty)

    def launcherContextFor(privilege: SecurityContext.Privilege): ExecutionContext =
      build(privilege, Set(Capability(TextusControlCenterLauncherRegistrationAuthenticationProvider.CAPABILITY)))

    def operatorCapabilityContext: ExecutionContext =
      build(SecurityContext.Privilege.User, Set(Capability(SecurityContext.Privilege.Operator.name)))
  }
}
