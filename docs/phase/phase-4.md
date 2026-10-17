# Phase 4 - Standalone Operational Component Control

Stage Status:

- Current status: CLOSED
- Current step: All twenty declared Steps have accepted exact local commits.
  The checklist is complete. Final validation and the sole epoch-2
  comprehensive review with focused blocker closure are accepted. The final
  release binds this canonical status and the retained follow-up journal.
- Owner: Textus Control Center Phase 4
- Update rule: Update this block and `phase-4-checklist.md` whenever a stable
  checklist state changes. Do not begin implementation before the lifecycle
  contracts are accepted by Textus Control Center, CNCF launcher, and Textus
  launcher.

status = closed

Approved final-validation owner disposition (Oct. 17, 2026): the current
shared-workflow main is the development and executable-validation owner; the
old checkout is retained for history only. The actual human decision explicitly
excludes its failed legacy validation from the current release gate without
claiming a pass. Its original base and accepted CC4-S0C eight-path/CC4-S0D
integration lineage, all thirteen roots and original bases, all twenty accepted
Steps, the same Goal and epoch-2, review history, counters and failures remain
retained. Canonical/Scala/runtime acceptance, the sole independent epoch-2
comprehensive review and exact release requirements remain in force.

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
- Define `textus-supervisor` as the lifecycle authority. In standalone mode it
  runs inside the local Control Center deployment; a later Compose/Kubernetes
  deployment uses the same supervisor contract as a separately placed service.
- Keep both Launchers responsible for durable common evidence and bounded
  supervisor notification only. They neither host lifecycle authority nor make
  Control Center availability a server-start prerequisite.

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
- The existing launcher-private `LifecycleSupervisor` is a transition baseline,
  not the accepted Phase 4 authority. It must be removed or reduced to a
  compatibility adapter before the reopened phase can close.

## 4. Contract Documents

- `docs/design/operational-component-management.md`
- `docs/design/launcher-registration.md`
- `docs/spec/operational-launch-profile.md`
- `docs/guide/standalone-operations.md`

## 5. Active Work Stack

- A (RETAINED): OC-01 - Freeze operating-target identity, adoption, exclusion,
  and runtime-projection rules.
- B (RETAINED): OC-02 - Preserve canonical target-first/current-directory launch
  identity and safe source/default-port facts.
- C (RETAINED): OC-03 - Persist shared CNCF/Textus Launcher server evidence before
  Control Center notification, including start, last-seen, normal-stop, and
  launcher-kind facts.
- D (RETAINED): OC-04 - Implement the approved `cncf launcher evidence list
  --format json` safe list/detail projection and Control Center reconciliation.
- E (DONE): OC-05 - Replace the provisional launcher-private supervisor
  with `textus-supervisor`, while preserving `cncf server` and
  `textus <artifact> server` as public start interfaces.
- F (DONE): OC-06 - Complete operating-panel and operator-guide
  acceptance around `textus-supervisor`, launcher evidence and canonical
  commands; CC4-S9 accepts the actual panel actions in both placements.
- G (DONE): OC-07 - Complete standalone acceptance with Java 17/default
  dependency inputs, full affected suites and the final Phase review.

Detailed progress and acceptance evidence belong in `phase-4-checklist.md`.

## 6. Approved CC4-S11 Audit Read Scope

Status: CLOSED (Oct. 17, 2026)

The resolved Phase-control decision preserves configured Production Web jobs
reads for `audit_viewer` through a dedicated canonical Audit scope. Operator
continues to require `job_admin` or `content_admin`. Audit requires the existing
enabled Production jobs policy, trusted provider authentication, system
privilege and configured jobs roles. It grants no management capability.

Acceptance requires canonical bounded list/detail, subject/scope-bound cursors,
no result payload/task/timeline reads, no diagnostics/control effects even with
mixed capabilities, manual refresh, default REST Audit denial, preserved
Operator behavior and original JVM constructors. The existing failing
`StaticFormAppRendererAuthorizationSpec` oracle remains unchanged.

The original nineteen accepted Steps and all prior epoch review histories
remain retained. Failed final validation remains recorded and unresolved;
CC4-S11 implementation, focused validation and independent Step acceptance are
complete; the sole Given/When/Then marker finding is resolved under the parent
M0 waiver. Exact local Step commits are complete: CNCF
`5c092bedaecab39a27bf3fb1397d1d889d297423` and primary
`c2492598beb91f8d6083114c68f420c4e5c1cdbe`. At that Step acceptance, Phase closure,
final full validation and the epoch-2 comprehensive Phase review remained pending;
the current final-validation disposition is recorded above. The same Goal,
original bases, counters, ledger and failure history remain retained. Phase 5/6,
publication, deployment and push remain excluded.

## 7. Final Closure

All OC-01 through OC-07 checklist obligations and the approved CC4-S11 scope
are complete. The final independent review's normal Launcher test-selection
finding is closed by the prescribed selector changes and independent focused
acceptance. The current canonical shared-workflow main remains the development
and validation owner; the legacy checkout and its failed validation remain
historical under the human-approved exclusion above.

Earlier pending/reopened summaries are retained history and are superseded by
this closure and the current checklist. The original thirteen repository bases,
twenty accepted Steps, same Goal and epoch-2, review and repair histories remain
retained. The thirty-two nonblocking Hygiene records retain their exact
dispositions in `../journal/2026/10/2026-10-05-phase-4-hygiene-follow-up.md`;
there are no Development Candidate entries or unpersisted accepted findings.
Phase 5 and Phase 6 have not started under this invocation.
