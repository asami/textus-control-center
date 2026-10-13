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

## Failure-boundary presentation

Control Center must show the availability actually achieved by layer instead of
collapsing everything into one HA indicator. At minimum distinguish Domain
Subsystem runtime placement from DataStore/stateful dependency availability.

For example, runtime local-ha with a single-host DataStore is not system-wide
host HA. Runtime host-ha also requires compatible availability from required
stateful dependencies before the whole system can be described as host-failure
tolerant.

Multi-AZ/site status is future scope. Phase 7 must not invent AZ topology or
cross-region controls before the CNCF/system-wide availability architecture
defines them.

## Deployment Transaction operations

Control Center presents CAR update as a Deployment Transaction rather than a
single deploy button/event. It should show candidate/admission, warming/probe,
switch, probationary Active, fallback-retention deadline, deployment commit or
switch-back, and final retirement/rebuild.

During the retention window, show both the new probationary Active and the
known-good retained former Active, including their CAR versions. Operators
should be able to see why an automatic-compatible update was admitted and which
compatibility limitation prevents automation.

A normal production profile should make this operation largely declarative:
selecting the standard availability/release policy should enable automatic
compatible CAR replacement and the configured fallback window without requiring
operators to author deployment/rollback scripts.
