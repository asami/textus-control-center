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
