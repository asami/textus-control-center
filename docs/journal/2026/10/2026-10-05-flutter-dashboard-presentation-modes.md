# Flutter Dashboard Presentation Modes

Date: 2026-10-05
Status: architectural direction
Related: Phase 5, macOS Menu Bar and Operational View

## Decision

The future Flutter Control Center client is a Presentation Subcomponent of the Control Center Subsystem, shared by Android and iPhone. It is not only a conventional mobile client: the same application may act as a desk-side operational dashboard on a tablet or a phone placed on a charging stand.

Define presentation modes conceptually as:

- Mobile: ordinary interactive mobile use.
- Dashboard: persistent information-dense tablet/large-surface operation.
- Stand: compact persistent presentation suitable for a phone on a stand.
- Ambient: low-information unattended presentation used after inactivity.

Mode selection is policy-driven. Charging/power state is an input, not the mode itself. Usable display regions, size, orientation, power state, inactivity, capabilities and explicit user preference may participate. Do not branch business logic on device names or Android/iOS identity.

A representative policy may choose Stand for a charging landscape phone, Dashboard for an admitted persistent tablet configuration, and Ambient after inactivity. Exact thresholds remain presentation policy/configuration.

## Ambient and burn-in mitigation

Ambient mode should minimize continuously illuminated content and may use a dark/black background, reduced information density/brightness and bounded position shifting. Touch or another admitted interaction restores an interactive mode.

Burn-in mitigation, inactivity timers and wake-lock/persistent-display mechanics are client presentation/runtime concerns. They are not Control Center domain logic and must not be encoded in CNCF Display Model semantics.

## Model boundary

Control Center remains authoritative for operational interpretation and exposes the same Operational Model/read APIs. Server-side Detail/Compact/Glance projections may express information priority. Flutter chooses a suitable visual realization and presentation mode from target-neutral semantics plus local environment facts.

Target shape:

Control Center Operational Model
-> Operational API / CNCF Display Model where applicable
-> Flutter Control Center Presentation Subcomponent
-> Presentation Mode resolver
-> Mobile | Dashboard | Stand | Ambient

The server does not infer charging state or own screen burn-in policy.

## Framework allocation

- textus-flutter-core: low-level power/charging, display, orientation and related environment observations/capabilities.
- textus-flutter-application-framework: reusable presentation-mode resolution and Ambient/persistent-dashboard behavior.
- Control Center Flutter application: application policy, mode configuration and operational composition.
- CNCF Display Model: target-neutral display semantics and information priority; no Flutter, charging or burn-in concepts.

This gives one application codebase multiple operational forms without making physical device categories part of the semantic model.
