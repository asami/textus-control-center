/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 */
package org.simplemodeling.textus.controlcenter

import java.time.{Clock, Duration, Instant, ZoneOffset}
import cats.~>
import org.goldenport.Consequence
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.cncf.action.{Action, ActionCall}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.config.RuntimeConfig
import org.goldenport.cncf.context.{Capability, DataStoreContext, EntityStoreContext, ExecutionContext, ExecutionProfileResolver, GlobalRuntimeContext, Principal, PrincipalId, RuntimeContext, ScopeContext, ScopeKind, SecurityContext}
import org.goldenport.cncf.datastore.{DataStore, DataStoreSpace}
import org.goldenport.cncf.entity.EntityStoreSpace
import org.goldenport.cncf.event.EventEngine
import org.goldenport.cncf.path.AliasResolver
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.unitofwork.{CommitRecorder, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.controlcenter.impl.{OperationalSummaryServiceFactoryImpl, TextusControlCenterLauncherRegistrationAuthenticationProvider}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class OperationalSummaryActionSpec extends AnyWordSpec with GivenWhenThen with Matchers {
  "OperationalSummary Actions" should {
    "normal generated read operation" which {
      "project actual component metadata, health, and registered leases without private fields" in {
        Given("a bootstrapped component with configured version, an in-memory launcher/operator store, and distinct registration and heartbeat receipt instants")
        val fixture = new Fixture()
        val component = _component(Configuration(Map(
          "component.version" -> ConfigurationValue.StringValue("7.8.9-summary-spec"),
          "component.health.checks" -> ConfigurationValue.StringValue("summary-check")
        )))
        val registration = fixture.contextFor(SecurityContext.Privilege.Internal, launcher = true, observedAt = _now.minusSeconds(1))
        val launcher = fixture.contextFor(SecurityContext.Privilege.Internal, launcher = true)
        val operator = fixture.contextFor(SecurityContext.Privilege.Operator)

        When("a launcher registers and heartbeats with advancing fixture receipt chronology, then the operator invokes normal getSummary and meta.version")
        val registered = _execute(component, registration, _registration_request("registerSubsystem"))
        val heartbeat = _execute(component, launcher, _registration_request("heartbeatSubsystem"))
        val resolved = component.logic.makeOperationRequest(_summary_request())
        val result = _execute(component, operator, _summary_request())
        val version = _execute(component, operator, Request.ofService("meta", "version"))

        Then("generated resolution and the shared response use actual component sources and safe registry data")
        registered.toOption should not be empty
        heartbeat.toOption should not be empty
        resolved.toOption.getOrElse(fail("getSummary did not resolve")) shouldBe a[TextusControlCenterComponent.OperationalSummaryService.GetSummary]
        val summary = _response(result)
        summary.getInt("protocolVersion") shouldBe Some(1)
        summary.getString("componentId") shouldBe Some(component.core.componentId.toString)
        summary.getString("instanceId") shouldBe Some(component.core.instanceId.toString)
        summary.getString("observedAt") shouldBe Some(operator.clock.instant().toString)
        val controlcenter = _record(summary, "controlCenter")
        controlcenter.getString("status") shouldBe Some("available")
        _record(controlcenter, "version").getString("value") shouldBe _response(version).getString("version")
        _record(controlcenter, "version").getString("value") shouldBe Some("7.8.9-summary-spec")
        _record(controlcenter, "uptime").getString("state") shouldBe Some("unavailable")
        _record(controlcenter, "uptime").getAny("seconds") shouldBe empty
        _record(controlcenter, "runtimeVersion").getString("state") shouldBe Some("unavailable")
        val health = _record(summary, "overallHealth")
        health.getString("status") shouldBe Some(component.healthSnapshot.status)
        health.getString("scope") shouldBe Some("control-center")
        _records(health, "checks").flatMap(_.getAny("detail")) shouldBe empty
        val runtimes = _record(summary, "runtimes")
        runtimes.getInt("total") shouldBe Some(1)
        runtimes.getInt("returned") shouldBe Some(1)
        runtimes.getInt("running") shouldBe Some(1)
        runtimes.getString("health") shouldBe Some("unavailable")
        val entry = _records(runtimes).head
        entry.getString("instanceId") shouldBe Some("summary-action-instance")
        entry.getString("artifactId") shouldBe Some("summary-artifact")
        entry.getString("status") shouldBe Some("running")
        Vector("registrationPrincipalId", "credentials", "developmentDirectory", "baseUrl", "hostLabel", "applicationUrl", "diagnostic")
          .foreach(name => entry.getAny(name) shouldBe empty)
        summary.getString("dashboardUrl") shouldBe Some("/web/textus-control-center")
        summary.getInt("refreshAfterSeconds") shouldBe Some(15)
      }

      "read actual boot metadata only from the current execution ancestry" in {
        Given("a selected runtime with an actual global ancestor separated by a child scope")
        val base = ExecutionContext.create(Clock.fixed(_now, ZoneOffset.UTC))
        val profile = ExecutionProfileResolver.resolveForSpec(_configuration(Configuration(Map(
          RuntimeConfig.EXECUTION_PROFILE_KEY -> ConfigurationValue.StringValue("controlled"),
          RuntimeConfig.EXECUTION_KEY -> ConfigurationValue.StringValue("summary-selected-runtime"),
          RuntimeConfig.EXECUTION_RANDOM_SEED_KEY -> ConfigurationValue.StringValue("summary-selected-runtime"),
          RuntimeConfig.EXECUTION_TIME_START_AT_KEY -> ConfigurationValue.StringValue(_now.minusSeconds(120).toString)
        )))).toOption.getOrElse(fail("summary spec execution profile did not resolve"))
        val global = GlobalRuntimeContext.create(
          "summary-selected-runtime", RuntimeConfig.default.copy(executionProfile = profile),
          _configuration(Configuration.empty), base.observability, AliasResolver.empty
        )
        global.executionProfileRuntime.testControl.getOrElse(fail("summary spec clock control unavailable"))
          .advanceBy(Duration.ofSeconds(120))
        val fixture = new Fixture(Some(global.createChildScope(ScopeKind.Runtime, "summary-child")))
        val component = _component(scope = Some(global.createChildScope(ScopeKind.Runtime, "summary-component-runtime")))
        val context = fixture.contextFor(SecurityContext.Privilege.Operator)

        When("the operator reads the Summary through the generated action")
        val result = _execute(component, context, _summary_request())

        Then("runtime version and uptime refer to that ancestor and the captured request clock")
        val summary = _response(result)
        val controlcenter = _record(summary, "controlCenter")
        _record(controlcenter, "runtimeVersion").getString("value") shouldBe Some(global.runtimeVersion)
        _record(controlcenter, "uptime").getString("state") shouldBe Some("available")
        _record(controlcenter, "uptime").getAny("seconds") shouldBe
          Some(Duration.between(global.bootedAt, Instant.parse(summary.getString("observedAt").get)).getSeconds.max(0L))
      }
    }

    "administrative read admission" which {
      "reject guests, ordinary users, and launcher-only credentials before internal reads" in {
        Given("a real component and guest, ordinary user, and launcher-only security contexts")
        val fixture = new Fixture()
        val component = _component()
        val contexts = Vector(
          fixture.contextFor(SecurityContext.Privilege.Anonymous),
          fixture.contextFor(SecurityContext.Privilege.User),
          fixture.contextFor(SecurityContext.Privilege.User, launcher = true)
        )

        When("each unadmitted caller invokes getSummary")
        val results = contexts.map(context => _execute_counted(component, context, _summary_request()))

        Then("each result denies inventory authority and no internal entity search executes")
        results.foreach { result =>
          result.toOption shouldBe empty
          result.toString should include ("administrative inventory authorization")
        }
        fixture.readCount shouldBe 0
      }

      "accept the existing inventory privileges, operator capability, and operator role" in {
        Given("authenticated inventory administrators and users with an operator capability or role")
        val fixture = new Fixture()
        val component = _component()
        val contexts = Vector(
          SecurityContext.Privilege.ApplicationContentManager,
          SecurityContext.Privilege.Operator,
          SecurityContext.Privilege.System,
          SecurityContext.Privilege.Internal
        ).map(privilege => fixture.contextFor(privilege)) ++ Vector(
          fixture.contextFor(SecurityContext.Privilege.User, capabilities = Set(Capability("operator"))),
          fixture.contextFor(SecurityContext.Privilege.User, attributes = Map("role" -> "operator"))
        )

        When("each admitted caller reads the empty registry Summary")
        val results = contexts.map(context => _execute_counted(component, context, _summary_request()))

        Then("the normal query succeeds with an empty bounded runtime collection")
        results.foreach { result =>
          _record(_response(result), "runtimes").getInt("total") shouldBe Some(0)
        }
        fixture.readCount shouldBe contexts.size
      }

      "reject invalid bounds before reading entities" in {
        Given("an admitted operator and both invalid edges of the compact limit")
        val fixture = new Fixture()
        val component = _component()
        val context = fixture.contextFor(SecurityContext.Privilege.Operator)

        When("the operator requests zero or one hundred and one entries")
        val results = Vector(0, 101).map(limit => _execute_counted(component, context, _summary_request(Some(limit))))

        Then("both requests are structured invalid operations without an internal search")
        results.foreach { result =>
          result.toOption shouldBe empty
          result.toString should include ("limit must be between 1 and 100")
        }
        fixture.readCount shouldBe 0
      }

      "require the actual component after authorization without manufacturing identity" in {
        Given("a generated Summary action but no component in its ActionCall core")
        val fixture = new Fixture()
        val component = _component()
        val action = component.logic.makeOperationRequest(_summary_request()).toOption.getOrElse(fail("summary action missing"))
          .asInstanceOf[TextusControlCenterComponent.OperationalSummaryService.GetSummary]
        val factory = OperationalSummaryServiceFactoryImpl()
        val operator = fixture.contextFor(SecurityContext.Privilege.Operator)
        val guest = fixture.contextFor(SecurityContext.Privilege.Anonymous)

        When("an operator and a guest execute the factory call without component context")
        val admitted = factory.createGetSummaryActionCall(ActionCall.Core(action, operator, None, None), action).execute()
        val denied = factory.createGetSummaryActionCall(ActionCall.Core(action, guest, None, None), action).execute()

        Then("the operator sees a structured missing-context failure while the guest is rejected first")
        admitted.toOption shouldBe empty
        admitted.toString should include ("requires the current component context")
        denied.toOption shouldBe empty
        denied.toString should include ("administrative inventory authorization")
        fixture.readCount shouldBe 0
      }
    }
  }

  private val _now = Instant.parse("2026-10-18T00:00:00Z")

  private final class Fixture(parent: Option[ScopeContext] = None) {
    private val _datastores = new DataStoreSpace().addDataStore(DataStore.inMemorySearchable())
    private val _entitystores = EntityStoreSpace.create(_configuration(Configuration.empty))
    private val _base = ExecutionContext.create(Clock.fixed(_now, ZoneOffset.UTC))
    private val _core = RuntimeContext.core(
      name = "summary-action-spec", parent = parent, observabilitycontext = _base.observability,
      datastore = Some(DataStoreContext(_datastores)), entitystore = Some(EntityStoreContext(_entitystores))
    )
    private var _reads = 0

    def readCount: Int = _reads

    def contextFor(
      privilege: SecurityContext.Privilege,
      launcher: Boolean = false,
      capabilities: Set[Capability] = Set.empty,
      attributes: Map[String, String] = Map.empty,
      observedAt: Instant = _now
    ): ExecutionContext = {
      val base = ExecutionContext.create(Clock.fixed(observedAt, ZoneOffset.UTC))
      val extra = if (launcher) Set(Capability(TextusControlCenterLauncherRegistrationAuthenticationProvider.CAPABILITY)) else Set.empty[Capability]
      val principalattributes = privilege.attributes ++ attributes + ("access_token" -> "private-summary-token")
      // Attach the counting runtime after security selection, which rebinds the UoW interpreter.
      lazy val context: ExecutionContext = ExecutionContext.withRuntimeContext(
        ExecutionContext.withSecurityContext(
          base,
          SecurityContext(
            principal = new Principal {
              def id: PrincipalId = privilege.principalId
              def attributes: Map[String, String] = principalattributes
            },
            capabilities = privilege.capabilities ++ capabilities ++ extra,
            level = privilege.level, subjectKind = privilege.subjectKind
          )
        ),
        runtime
      )
      lazy val uow = new UnitOfWork(context, EventEngine.noop(DataStore.noop()), CommitRecorder.noop)
      lazy val interpreter = new UnitOfWorkInterpreter(uow)
      lazy val unitofworkinterpreter = new (UnitOfWorkOp ~> Consequence) {
        def apply[A](operation: UnitOfWorkOp[A]): Consequence[A] = {
          operation match {
            case _: UnitOfWorkOp.EntityStoreSearchInternal[?] => _reads += 1
            case _ => ()
          }
          Consequence(interpreter.execute(operation))
        }
      }
      lazy val runtime = new RuntimeContext(
        core = _core, unitofworksupplier = () => uow, unitofworkinterpreterfn = unitofworkinterpreter,
        commitaction = unitofwork => { val _ = unitofwork.commit(); () },
        abortaction = unitofwork => { val _ = unitofwork.rollback(); () },
        disposeaction = _ => (), token = "summary-action-spec"
      )
      context
    }
  }

  private def _configuration(configuration: Configuration): ResolvedConfiguration =
    ResolvedConfiguration(configuration, ConfigurationTrace.empty)

  private def _component(configuration: Configuration = Configuration.empty, scope: Option[ScopeContext] = None): Component = {
    val subsystem = Subsystem(name = "summary-action-spec", configuration = _configuration(configuration), scopeContext = scope)
    val bundle = new impl.ComponentFactory().create(ComponentCreate(subsystem, ComponentOrigin.Main))
    val component = new org.goldenport.cncf.component.ComponentFactory().bootstrap(bundle.participants.head)
    subsystem.add(component)
    component
  }

  private def _execute(component: Component, context: ExecutionContext, request: Request): Consequence[OperationResponse] =
    component.logic.makeOperationRequest(request).flatMap {
      case action: Action => component.actionEngine.execute(component.logic.createActionCall(action, context))
      case _ => Consequence.operationInvalid("request did not resolve to an action")
    }

  // Retain the fixture's counting interpreter; the normal action-scope route is covered separately.
  private def _execute_counted(component: Component, context: ExecutionContext, request: Request): Consequence[OperationResponse] =
    component.logic.makeOperationRequest(request).flatMap {
      case action: Action =>
        component.actionEngine.execute(action.createCall(ActionCall.Core(action, context, Some(component), context.observability.correlationId)))
      case _ => Consequence.operationInvalid("request did not resolve to an action")
    }

  private def _summary_request(limit: Option[Int] = None): Request =
    Request.ofService("OperationalSummary", "getSummary", properties = limit.map(value => Property("limit", value, None)).toList)

  private def _registration_request(operation: String): Request =
    Request.ofService("SubsystemInventory", operation, properties = List(
      Property("protocolVersion", 1, None), Property("instanceId", "summary-action-instance", None),
      Property("launcherKind", "textus", None), Property("target", "summary-target", None),
      Property("artifactId", "summary-artifact", None), Property("executionMode", "development", None),
      Property("developmentDirectory", "/private/summary-work", None),
      Property("subsystemName", "SummarySubsystem", None), Property("subsystemVersion", "1.2.3", None),
      Property("runtimeVersion", "4.5.6", None), Property("baseUrl", "https://private.example.test", None),
      Property("applicationUrl", "https://private.example.test/web/private", None),
      Property("hostLabel", "private-summary-host", None), Property("startedAt", _now.minusSeconds(10), None),
      Property("launcherState", if (operation == "registerSubsystem") "starting" else "running", None)
    ))

  private def _response(result: Consequence[OperationResponse]): Record =
    result.toOption.collect { case OperationResponse.RecordResponse(record) => record }
      .getOrElse(fail(s"Summary operation failed: $result"))

  private def _record(record: Record, name: String): Record =
    record.getAny(name).collect { case value: Record => value }.getOrElse(fail(s"$name record missing"))

  private def _records(record: Record, name: String = "data"): Vector[Record] =
    record.getAny(name).collect { case values: Seq[?] => values.collect { case value: Record => value }.toVector }
      .getOrElse(Vector.empty)
}
