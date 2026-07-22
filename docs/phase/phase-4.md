# Phase 4 - Standalone Operational Component Control

Stage Status:

- Current status: COMPLETE
- Current step: Closed with canonical `cncf server` and `textus <artifact>
  server` shared-evidence reconciliation, Launcher-owned lifecycle authority,
  and the standalone operating panel.
- Owner: Textus Control Center Phase 4
- Update rule: Update this block and `phase-4-checklist.md` whenever a stable
  checklist state changes. Do not begin implementation before the lifecycle
  contracts are accepted by Textus Control Center, CNCF launcher, and Textus
  launcher.

status = completed

## 1. Purpose

Phase 4 turns the standalone Control Center home page into an operating panel
for components, rather than only an inventory of individual launcher
invocations. It provides a durable local operating-target record and derives
runtime status from launcher registration plus launcher-owned local evidence.

Both CNCF Launcher and Textus Launcher retain one common local server-evidence
record under `~/.cncf/launcher/server-evidence.json` before they attempt any
Control Center notification. The record therefore survives an unavailable or
not-yet-installed Control Center. Control Center later obtains a safe
projection through a Launcher interface and decides whether the evidence is a
current or historical invocation.

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
  invocation history, launcher evidence state, lifecycle request state, and
  last result through command, REST, and the static Web application.
- Keep `cncf server` (from the current development directory) and
  `textus <artifact> server` as the public canonical start interfaces; they
  must not require a separate `cncf launcher supervisor serve` command.
- Define the versioned shared evidence schema, its safe projection, and
  launcher-mediated reconciliation semantics for `launcherKind`, target,
  execution mode, development directory, instance identity, and lifecycle
  timestamps.
- Reconcile launcher evidence at Control Center startup and on bounded refresh,
  without reading `~/.cncf/launcher/` directly from Control Center.
- Add Start, Stop, Restart, Remove from management, and Restore to management
  Operations.
- Redefine lifecycle authority so a Control Center action can affect only a
  Launcher-managed invocation. It must never infer ownership from a PID or make
  the former foreground supervisor command a user-facing prerequisite.

## 3. Boundaries

- Textus Control Center does not search the operating system for a process,
  infer PID ownership, or signal an arbitrary PID.
- Textus Control Center does not read or write the shared evidence file
  directly. The file is Launcher-owned; only a safe Launcher projection crosses
  the boundary.
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
- `docs/design/launcher-registration.md`
- `docs/spec/operational-launch-profile.md`
- `docs/guide/standalone-operations.md`

## 5. Active Work Stack

- A (DONE): OC-01 - Freeze operating-target identity, adoption, exclusion,
  and runtime-projection rules.
- B (DONE): OC-02 - Preserve canonical target-first/current-directory launch
  identity and safe source/default-port facts.
- C (DONE): OC-03 - Persist shared CNCF/Textus Launcher server evidence before
  Control Center notification, including start, last-seen, normal-stop, and
  launcher-kind facts.
- D (DONE): OC-04 - Implement the approved `cncf launcher evidence list
  --format json` safe list/detail projection and Control Center reconciliation.
- E (DONE): OC-05 - Replace the foreground-supervisor lifecycle
  assumption with a Launcher-managed lifecycle authority that preserves
  `cncf server` as the public start interface. The authority is ensured only
  internally by canonical Launcher/Control Center flows; operators never run
  `cncf launcher supervisor serve` as a prerequisite.
- F (DONE): OC-06 - Generate and implement Control Center records,
  Operations, authorization, and projections.
- G (DONE): OC-07 - Revise the operating-panel Web UI and operator guide
  around launcher evidence and the canonical commands.
- H (DONE): OC-08 - Run standalone acceptance, failure-isolation, and
  cross-repository executable specifications; close the phase.

Detailed progress and acceptance evidence belong in `phase-4-checklist.md`.
