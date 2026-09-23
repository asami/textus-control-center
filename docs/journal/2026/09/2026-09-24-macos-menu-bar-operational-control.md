# macOS Menu Bar and Operational Control

Date: 2026-09-24

## Decision

Textus Control Center will act as the operational entry point for the Textus execution environment, covering applications/runtime plus configured related middleware such as PostgreSQL and OpenTelemetry/observability services.

The existing Web Dashboard remains part of Control Center itself. A new Swift/SwiftUI macOS Menu Bar subproject provides a compact native presentation and Dashboard launcher.

The Menu Bar app is deliberately thin: it reads a general Control Center Operational View over HTTP/JSON. Health discovery and operational semantics remain in Control Center.

The native application is a platform-specific Presentation Subcomponent and participates in the CNCF CAR platform-Subcomponent distribution model. It may be bundled in the main CAR or shipped as a separate CAR.

## Initial boundary

Phase 5 covers read-only status, middleware observation, Dashboard launch and packaging/integration. Mutating controls such as start/stop/restart can be added later.

## Host profile clarification

The Mac mini is the server-oriented host: PostgreSQL runs as separate `ops` and `dev` instances and OpenTelemetry/observability is enabled. The MacBook Air does not use local PostgreSQL or OpenTelemetry. Control Center therefore treats middleware monitoring as host-profile/configuration driven; absence on the MacBook Air is not an unhealthy state.

## Multi-device presentation direction

The Control Center presentation roadmap now includes Flutter mobile clients for iPhone/Android and wearable clients for Apple Watch and Pixel Watch. Mobile UI is shared with Flutter. Wearables are not required to share a Flutter implementation: watchOS may use Swift/SwiftUI and Wear OS Kotlin/Compose. The reusable boundary is the Operational API/read model, not the widget toolkit.

A future Glance View should keep wearable applications thin and optimized for overall health, alerts and Job/Workflow status. Device-specific notification and quick-action work is deferred.
