# Phase 7: Subsystem Availability and Immutable Runtime Operations

Status: PLANNED
Planned: 2026-10-14
Depends on: CNCF Phase 108
Authority: CNCF Subsystem Runtime Architecture / Availability Architecture
Related: Textus Security Operations

## Goal

Present and operate CNCF Active/Standby Domain Subsystem availability without
reimplementing CNCF runtime authority.

## Work

- show Active/Standby identity, CAR/runtime version and lifecycle/readiness;
- distinguish process alive, READY, ACTIVE mutation authority and STANDBY;
- show warm-up/cache readiness where operationally meaningful;
- show last successful Active rotation/takeover and failed attempt/reason;
- show drain progress, degraded/single-instance state and durable Job/Workflow
  recovery status;
- provide authorized operations for planned rotation/replacement and
  security-triggered replacement through CNCF operations;
- surface Standby read-only/cache workload as functional capacity rather than
  treating all non-Active instances as idle;
- integrate relevant OTLP/runtime events into the incident/availability
  timeline.

## Security boundary

Control Center does not grant Active authority itself and does not infer it from
health. It invokes CNCF's authorized transition operation. Security-triggered
replacement uses the ordinary replacement path.

Operational displays and event history must not expose credentials, tokens or
unnecessary business/personal payloads.

## Acceptance

An operator can distinguish a healthy-but-not-ready Standby from a takeover-ready
one, see when each instance last successfully served as Active, request an
authorized planned rotation, observe switch/drain/recovery, and identify a
failed takeover without a separate emergency-only UI path.

## Declarative profile operations

Control Center presents the configured availability profile and its effective
runtime state. Operators may select/adjust authorized profile policy, but
Control Center does not translate that into its own A/B orchestration script;
CNCF/Launcher remains the lifecycle owner.

The UI should make the common case simple: selecting the standard profile is
enough to request managed Active/Standby operation and automatic rotation.
Advanced fields expose only meaningful overrides such as rotation cadence or
Standby query policy.

Rotation history records the trigger/reason (scheduled, release, failure,
security or operator) while presenting one common replacement lifecycle.
