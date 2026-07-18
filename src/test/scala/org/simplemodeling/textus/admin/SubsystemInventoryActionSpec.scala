/*
 * @version Jul. 18, 2026
 */
package org.simplemodeling.textus.admin

import cats.~>
import org.goldenport.Consequence
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ResolvedConfiguration}
import org.goldenport.cncf.action.Action
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.{Capability, DataStoreContext, EntityStoreContext, ExecutionContext, Principal, PrincipalId, RuntimeContext, SecurityContext}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.EntityStoreSpace
import org.goldenport.cncf.event.EventEngine
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.unitofwork.{CommitRecorder, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.admin.impl.TextusAdminLauncherRegistrationAuthenticationProvider
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant

final class SubsystemInventoryActionSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "SubsystemInventory Actions" should {
    "persist launcher reports and return only safe administrative projections" in {
      Given("an in-memory Textus Admin component with launcher and operator principals")
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
      registered.getString("instanceId") shouldBe Some("textusadminactionspec")
      registered.getString("status") shouldBe Some("starting")
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
        Request.ofService("SubsystemInventory", "getSubsystem", properties = List(Property("instanceId", "textusadminactionspec", None)))
      ).toOption.getOrElse(fail("detail failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("both administrative reads expose the same safe instance")
      _records(listed).map(_.getString("instanceId")) shouldBe Vector(Some("textusadminactionspec"))
      loaded.getString("instanceId") shouldBe registered.getString("instanceId")
      loaded.getAny("registrationPrincipalId") shouldBe empty

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
        Request.ofService("SubsystemInventory", "deregisterSubsystem", properties = List(Property("instanceId", "textusadminactionspec", None)))
      ).toOption.getOrElse(fail("deregister failed")).asInstanceOf[OperationResponse.RecordResponse].record

      Then("the instance is retained as stopped")
      heartbeated.getString("status") shouldBe Some("running")
      stopped.getString("status") shouldBe Some("stopped")
    }

    "reject administrative reads from a non-operator principal" in {
      Given("a Textus Admin component and a launcher-level principal")
      val fixture = _fixture()
      val component = _component()
      val launchercontext = fixture.launcherContextFor(SecurityContext.Privilege.User)

      When("the launcher attempts to list the inventory")
      val result = _execute(component, launchercontext, Request.ofService("SubsystemInventory", "listSubsystems"))

      Then("the operation returns a structured authorization failure")
      result.toOption shouldBe empty
    }

    "reject registration from an authenticated principal without launcher capability" in {
      Given("a Textus Admin component and a human-level authenticated principal")
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
  }

  private def _fixture(): _Fixture = {
    val datastore = DataStore.inMemorySearchable()
    val datastorespace = new DataStoreSpace().addDataStore(datastore)
    val entitystorespace = EntityStoreSpace.create(
      ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
    )
    val base = ExecutionContext.create()
    val core = RuntimeContext.core(
      name = "textus-admin-action-spec",
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
        token = "textus-admin-action-spec"
      )
      context
    }
    _Fixture(build)
  }

  private def _component(): Component = {
    val subsystem = Subsystem(
      name = "textus-admin-action-spec",
      configuration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
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

  private def _registration_request(operation: String, startedat: Instant): Request =
    Request.ofService(
      "SubsystemInventory",
      operation,
      properties = List(
        Property("protocolVersion", 1, None),
        Property("instanceId", "textusadminactionspec", None),
        Property("launcherKind", "textus", None),
        Property("target", "textusadmin", None),
        Property("subsystemName", "TextusAdmin", None),
        Property("subsystemVersion", "v010snapshot", None),
        Property("runtimeVersion", "v050", None),
        Property("baseUrl", "http://127.0.0.1:8080", None),
        Property("hostLabel", "actionspec", None),
        Property("startedAt", startedat, None),
        Property("launcherState", if (operation == "registerSubsystem") "starting" else "running", None)
      )
    )

  private def _records(record: Record): Vector[Record] =
    record.getAny("data") match {
      case Some(values: Vector[?]) => values.collect { case value: Record => value }
      case Some(values: Seq[?]) => values.collect { case value: Record => value }.toVector
      case Some(value: Record) => Vector(value)
      case _ => Vector.empty
    }

  private final case class _Fixture(
    build: (SecurityContext.Privilege, Set[Capability]) => ExecutionContext
  ) {
    def contextFor(privilege: SecurityContext.Privilege): ExecutionContext =
      build(privilege, Set.empty)

    def launcherContextFor(privilege: SecurityContext.Privilege): ExecutionContext =
      build(privilege, Set(Capability(TextusAdminLauncherRegistrationAuthenticationProvider.CAPABILITY)))
  }
}
