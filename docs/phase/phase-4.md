# Phase 4 - Standalone Operational Component Control

Stage Status:

- Current status: IN_PROGRESS
- Current step: Implement Control Center operating-target records and projections (OC-04).
- Owner: Textus Control Center Phase 4
- Update rule: Update this block and `phase-4-checklist.md` whenever a stable
  checklist state changes. Do not begin implementation before the lifecycle
  contracts are accepted by Textus Control Center, CNCF launcher, and Textus
  launcher.

status = planned

## 1. Purpose

Phase 4 turns the standalone Control Center home page into an operating panel
for components, rather than only an inventory of individual launcher
invocations. It provides a durable local operating-target record, derives
runtime status from launcher registration, and requests local lifecycle actions
from a launcher-owned supervisor.

An operating target is not the same as a running instance. A target remains in
the panel when stopped; only an explicit operator action removes it from
management. The phase preserves the Phase 3 separation between a logical CAR,
its sources, and its runtime invocations.

## 2. Scope

- Add an `OperationalComponent` model keyed by CAR `artifactId` and linked to
  the existing `ManagedCar` catalog identity.
- Automatically manage a CAR discovered from an explicitly registered
  development directory.
- Adopt a local or public CAR when an accepted launcher registration proves it
  was used.
- Persist an operator exclusion so a refresh does not re-add an explicitly
  removed development CAR.
- Project operating target, source availability, current runtime status,
  invocation history, lifecycle request state, and last result through command,
  REST, and the static Web application.
- Add Start, Stop, Restart, Remove from management, and Restore to management
  Operations.
- Route lifecycle Operations to an authorized local launcher supervisor;
  `textus-launcher` delegates to the same contract used by `cncf-launcher`.
- Use declared default ports and an explicit safe launch profile; reject
  port conflict, missing source, and unresolved-launch-profile cases before a
  start request is accepted.
- Add request identity, idempotency, timeout, structured result, and audit
  semantics for every lifecycle operation.

## 3. Boundaries

- Textus Control Center does not search the operating system for a process,
  infer PID ownership, or signal an arbitrary PID.
- A lifecycle request does not replace launcher registration: the resulting
  server still reports registration, heartbeat, and normal termination through
  the existing protocol.
- Removing a component from management does not stop a running instance.
- Initial standalone operation supports one selected default-port deployment per
  component. Multi-instance placement, remote hosts, scheduling, and rollout
  control are later work.
- The phase does not add detailed health/metric aggregation, remote repository
  mutation, publication, or multi-user source providers.

## 4. Contract Documents

- `docs/design/operational-component-management.md`
- `docs/spec/operational-launch-profile.md`
- `docs/spec/launcher-lifecycle-control.md`
- `docs/guide/standalone-operations.md`

## 5. Active Work Stack

- A (DONE): OC-01 - Freeze operating-target identity, adoption, exclusion,
  and runtime-projection rules.
- B (DONE): OC-02 - Freeze safe local launch-profile and default-port rules.
- C (DONE): OC-03 - Freeze the launcher-supervisor lifecycle protocol and
  cross-repository ownership.
- D (PLANNED): OC-04 - Generate and implement Control Center records,
  Operations, authorization, and projections.
- E (PLANNED): OC-05 - Implement the CNCF/Textus launcher supervisor adapters
  and registration correlation.
- F (PLANNED): OC-06 - Implement the operating-panel Web UI and operator guide.
- G (PLANNED): OC-07 - Run standalone acceptance, failure-isolation, and
  cross-repository executable specifications; close the phase.

Detailed progress and acceptance evidence belong in `phase-4-checklist.md`.
