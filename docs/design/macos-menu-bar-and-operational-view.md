# macOS Menu Bar and Operational View

## Architecture

Textus Control Center owns the operational model and Web Dashboard. The macOS Menu Bar application is a thin platform-specific Presentation Subcomponent.

The Menu Bar application communicates with Control Center over its normal HTTP/JSON interface. It must not duplicate service discovery or middleware health logic.

## Operational View

Provide a compact Control Center summary suitable for both the Web Dashboard and native presentations. Initial information includes:

- Control Center status, version and uptime
- overall health
- Textus application/runtime status summary
- related middleware status, initially including PostgreSQL and OpenTelemetry/observability services where configured
- Dashboard URL

The API/operation is general Control Center functionality, not a macOS-specific operation.

Use the existing operational-component/catalog identity and safe Launcher
projections. A reusable Summary must come from stable normal Operation/HTTP
contracts, not HTML or an implementation-private Dashboard response. Reuse
bounded runtime metrics already exposed by those contracts; do not create a
new collection platform for the initial view. Exact request/result/state names
are specified when the relevant producer interfaces are reconciled.

## macOS Subproject

Add a Swift/SwiftUI Menu Bar application subproject. Initial scope:

- Menu Bar status item
- read-only summary display
- refresh
- disconnected state
- Open Dashboard
- login-time operation through textus-launcher integration

Start/stop/restart, configuration editing, log browsing and notifications may follow later.

## Distribution

The built .app is a platform-specific Presentation Subcomponent. It may be bundled in textus-control-center.car or distributed in a separate CAR. Logical Subcomponent identity does not depend on the physical packaging choice.

## Host Profiles

Middleware observation is configuration/profile driven, not mandatory on every host.

- Mac mini server profile: PostgreSQL is enabled with separate `ops` and `dev` instances; OpenTelemetry/observability services are enabled.
- MacBook Air development/client profile: PostgreSQL and OpenTelemetry are not used and therefore are not expected or reported as failures.

Model middleware as service plus named instance/profile rather than hard-coding a single PostgreSQL process. Disabled/not-applicable services must be distinguished from stopped/unhealthy services.

These are configured profiles, not machine-name tests. An unavailable configured
observer is unknown/unavailable, not proof of healthy or stopped service. Client
disconnection is separate from the last observed server/service state.

## Docker and development observations

Configured Docker OPS services may include PostgreSQL, OpenTelemetry, Prometheus,
Grafana, Jaeger and cloudflared without requiring CAR/Launcher identities for
ordinary infrastructure. Keep deployment/container state, Launcher lease, CNCF
health/readiness and endpoint reachability distinct. Compose is desired topology,
not Control Center's runtime registry. Observation does not grant Docker socket
or lifecycle authority; Docker control belongs to Phase 8 and its Supervisor.

Development-CAR summaries use sm-workflow current state and authoritative CNCF
Service Bus journal events under the existing
[development operations boundary](development-operations.md). Display attributable
progress/attention and deep links; detailed control and decisions stay with their
owners. Phase 5.1 adds read-only projection, not Phase 6 admission actions, a remote
Service Bus endpoint, or another workflow/event store. Missing sources remain
explicit and do not become invented healthy/completed development state.

## Presentation Roadmap

Control Center presentations share the same server-side Operational Model and read APIs while keeping device-specific UI thin.

- Web Dashboard: Control Center Web application.
- macOS Menu Bar: Swift/SwiftUI compact presentation.
- Mobile: future Flutter application shared by iPhone and Android.
- Wearables: future Apple Watch and Pixel Watch targets.

Apple Watch and Pixel Watch are not forced through one Flutter UI implementation. Watch-specific presentation uses native stacks where appropriate: Swift/SwiftUI for watchOS and Kotlin/Compose for Wear OS. Commonality lives primarily in the Control Center API, JSON contracts, use cases and server-side read models.

Introduce presentation-oriented projections from the same Operational Model, for example Detail View, Compact View and Glance View. Wearable clients should be close to display/interaction adapters: health interpretation, resource monitoring and decision logic remain in Control Center. Watch scope should emphasize glanceable overall health, important alerts, active jobs/workflows and later carefully selected quick actions/notifications.

## Flutter persistent dashboard direction

The Flutter mobile Presentation Subcomponent is one application codebase with policy-selected presentation modes: Mobile, Dashboard, Stand and Ambient. These are presentation modes rather than device classes.

Mode resolution combines local environment facts such as usable display geometry, orientation, charging/power state, inactivity and capabilities with explicit application/user policy. Charging is a useful signal for desk/stand use but is not itself authority to enter Dashboard or Stand mode.

Ambient is the unattended persistent-display realization. It minimizes continuously illuminated content and may use a dark/black background, reduced information density/brightness and bounded position shifting. Interaction restores an interactive mode. Wake-lock, inactivity timing and burn-in mitigation remain Flutter/framework runtime responsibilities rather than server or domain logic.

The dependency boundary is: textus-flutter-core observes low-level device/power/display facts; TFAF resolves reusable presentation policy; the Control Center Flutter application supplies application composition/defaults; CNCF Display Model remains target-neutral and supplies semantic display information/priority where applicable.

## Initial Summary source boundary

The [Operational Summary contract](../spec/operational-summary.md) fixes the
initial shared read API: current Component identity/health, normal meta.version,
current execution ancestry runtime metadata, and bounded safe existing registry
lease projections. Overall health covers only Control Center; configured
middleware and remote observations remain PHASE-5.1. Web and native callers use
the same normal Operation/REST projection and resolve its relative Dashboard
URL against their selected API server. The first native distribution intent is
a parent-bundled CAR on an explicitly configured development/client Darwin
arm64 host; this does not assert implemented Launcher installer support.
