/*
 * @version Jul. 19, 2026
 */
package org.simplemodeling.textus.controlcenter

import cats.~>
import org.goldenport.Consequence
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.cncf.config.ResolvedParameters
import org.goldenport.cncf.action.Action
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{Capability, DataStoreContext, EntityStoreContext, ExecutionContext, Principal, PrincipalId, RuntimeContext, SecurityContext}
import org.goldenport.cncf.datastore.{ComponentDataStore, DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.EntityStoreSpace
import org.goldenport.cncf.event.EventEngine
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.unitofwork.{CommitRecorder, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.impl.TextusControlCenterLauncherRegistrationAuthenticationProvider
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.nio.file.{Files, Path, Paths}

final class SubsystemInventoryActionSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "SubsystemInventory Actions" should {
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
      _execute(
        firstcomponent,
        firstfixture.launcherContextFor(SecurityContext.Privilege.Internal),
        _registration_request("registerSubsystem", startedat)
      ).toOption should not be empty

      And("a newly constructed runtime opens the same local datastore")
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
      _records(result).map(_.getString("instanceId")) shouldBe Vector(Some("textuscontrolcenteractionspec"))
      _records(result).flatMap(_.getAny("registrationPrincipalId")) shouldBe empty
    }

    "refresh, list, and load managed CAR snapshots through the protected operation surface" in {
      Given("a configured standalone development root with one CAR descriptor")
      val root = Files.createTempDirectory("control-center-catalog")
      val car = Files.createDirectory(root.resolve("textus-catalog-spec"))
      Files.writeString(
        car.resolve("project.yaml"),
        "project:\n  name: textus-catalog-spec\n  kind: car\n  component:\n    name: catalog-spec-component\n"
      )
      val fixture = _fixture()
      val component = _component(_catalog_configuration(root))
      val operatorcontext = fixture.contextFor(SecurityContext.Privilege.ApplicationContentManager)
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.Internal)

      When("a launcher reports an artifact-identified running instance and an operator reads the catalog")
      _execute(component, launchercontext, _registration_request("registerSubsystem", Instant.parse("2026-07-21T00:00:00Z"), Some("textus-catalog-spec"))).toOption should not be empty
      _execute(component, launchercontext, _registration_request("heartbeatSubsystem", Instant.parse("2026-07-21T00:00:00Z"), Some("textus-catalog-spec"))).toOption should not be empty
      val refresh = _execute(component, operatorcontext, Request.ofService("CarCatalog", "refreshCarCatalog"))
        .toOption.getOrElse(fail("catalog refresh failed"))
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
  }

  private def _fixture(datastorepath: Option[Path] = None): _Fixture = {
    val datastorespace = datastorepath match {
      case Some(path) =>
        DataStoreSpace.default().useApplicationDataStore(
          ComponentDataStore.Environment(_resolved_parameters(path), Some(_standalone_configuration(path))),
          "TextusControlCenter",
          "application"
        )
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
    def build(privilege: SecurityContext.Privilege, extraCapabilities: Set[Capability]): ExecutionContext = {
      lazy val context: ExecutionContext = ExecutionContext.withSecurityContext(
        ExecutionContext.withRuntimeContext(base, runtime),
        SecurityContext(
          principal = new Principal {
            def id: PrincipalId = privilege.principalId
            def attributes: Map[String, String] =
              privilege.attributes + ("access_token" -> s"token-${privilege.principalId.value}")
          },
          capabilities = privilege.capabilities ++ extraCapabilities,
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
    _Fixture(build)
  }

  private def _component(configuration: ResolvedConfiguration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)): Component = {
    val subsystem = Subsystem(
      name = "textus-control-center-action-spec",
      configuration = configuration
    )
    val bundle = new impl.ComponentFactory().create(ComponentCreate(subsystem, ComponentOrigin.Main))
    val component = new org.goldenport.cncf.component.ComponentFactory().bootstrap(bundle.participants.head)
    subsystem.add(component)
    component
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

  private def _registration_request(operation: String, startedat: Instant, artifactid: Option[String] = None): Request =
    Request.ofService(
      "SubsystemInventory",
      operation,
      properties = List(
        Property("protocolVersion", 1, None),
        Property("instanceId", "textuscontrolcenteractionspec", None),
        Property("launcherKind", "textus", None),
        Property("target", "textus-control-center", None),
        Property("executionMode", "development", None),
        Property("developmentDirectory", "/work/textus-control-center", None),
        Property("subsystemName", "TextusControlCenter", None),
        Property("subsystemVersion", "v010snapshot", None),
        Property("runtimeVersion", "v050", None),
        Property("baseUrl", "http://127.0.0.1:8080", None),
        Property("hostLabel", "actionspec", None),
        Property("startedAt", startedat, None),
        Property("launcherState", if (operation == "registerSubsystem") "starting" else "running", None)
      ) ++ artifactid.map(value => Property("artifactId", value, None)).toList
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
        "textus.component.textus-control-center.datastores.application.policy" -> ConfigurationValue.StringValue("local-default"),
        "textus.local-data.textus-control-center.application.path" -> ConfigurationValue.StringValue(path.toString)
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

  private def _catalog_file_configuration(path: Path): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(
        "textus-control-center.catalog.file" -> ConfigurationValue.StringValue(path.toString)
      )),
      ConfigurationTrace.empty
    )

  private def _write_car_descriptor(root: Path, artifactid: String, componentname: String): Unit = {
    val project = Files.createDirectory(root.resolve(artifactid))
    Files.writeString(
      project.resolve("project.yaml"),
      s"project:\n  name: $artifactid\n  kind: car\n  component:\n    name: $componentname\n"
    )
  }

  private def _resolved_parameters(path: Path): ResolvedParameters =
    ResolvedParameters.fromResolvedConfiguration(
      ResolvedConfiguration(
        Configuration(Map(
          "textus.local-data.textus-control-center.application.path" -> ConfigurationValue.StringValue(path.toString)
        )),
        ConfigurationTrace.empty
      )
    )

  private final case class _Fixture(
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
