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

## Host profiles

- Mac mini: monitor PostgreSQL `ops` and `dev` independently and monitor configured OpenTelemetry/observability services.
- MacBook Air: PostgreSQL and OpenTelemetry are disabled/not applicable and must not produce unhealthy status.
- Represent middleware as configurable service instances rather than assuming one global instance.

## Future presentation targets (out of Phase 5 implementation)

- Flutter Control Center application for iPhone and Android.
- Apple Watch client using watchOS native presentation where appropriate.
- Pixel Watch client using Wear OS native presentation where appropriate.
- Reuse common Operational API/JSON and server-side Detail/Compact/Glance projections rather than forcing watch UI through Flutter.
- Keep wearable clients thin; monitoring and health interpretation remain server-side.
