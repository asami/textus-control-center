# Phase 5: macOS Menu Bar and Operational View

## Goal

Extend Textus Control Center into a compact operational entry point for the local Textus environment and provide a macOS Menu Bar presentation distributed as a platform Subcomponent.

## Scope

- Add a Swift/SwiftUI macOS Menu Bar application subproject.
- Define a reusable Control Center Operational/Summary View.
- Expose the view through the normal Control Center operation/HTTP JSON surface.
- Include Control Center status, version, uptime, overall health and Dashboard URL.
- Observe configured Textus applications/runtime.
- Observe configured related middleware, initially PostgreSQL and OpenTelemetry/observability services.
- Show summary, refresh, disconnected state and Open Dashboard from the Menu Bar application.
- Build the .app for CAR platform-Subcomponent packaging.
- Support both bundled-in-parent-CAR and separate-CAR distribution models through CNCF/launcher integration.

## Design constraints

- Web Dashboard remains a Control Center Web application.
- Menu Bar app remains a thin presentation; monitoring/business logic stays in Control Center.
- The Operational View is not macOS-specific and should be reusable by the Web Dashboard and future presentations.

## Deferred

- Start/stop/restart controls.
- Configuration editing.
- Log browsing.
- Native notifications and richer alerting.
