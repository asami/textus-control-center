status = draft
scope = internal development strategy

# Textus Control Center Development Strategy

## 1. Purpose

Textus Control Center provides an operational management subsystem for CNCF Subsystems
started through `textus-launcher` or `cncf-launcher`.

The first responsibility is cross-instance discovery. Textus Control Center does not
replace the System Admin pages already hosted by each Subsystem. It provides a
management-plane index over those independently running Subsystems and links
operators to the authoritative per-Subsystem administration surfaces.

## 2. Development Principles

- Build `textus-control-center` as a Cozy-generated CAR project.
- Keep lifecycle execution with the owning Supervisor and registration/evidence
  with Launchers, following accepted Phase 4; Textus Control Center must not infer
  ownership from arbitrary operating-system processes.
- Use one operation model and project it to command, REST, and Web UI.
- Keep the Web UI operation-backed. It must not maintain a separate UI-only
  registry or mutate runtime state directly.
- Treat launcher registration facts, observed runtime state, and requested
  lifecycle state as different records.
- Treat a logical CAR, its development/local/public sources, and its runtime
  instances as separate records. A CAR that is not running is not stale.
- Do not depend on deprecated `cncf dev server` behavior or
  `target/cncf.d/dev-server.json`.
- Support the canonical launcher forms:
  - `textus <artifact> server`
  - `cncf server`
  - `cncf <target> server`
- A Textus Control Center outage must not prevent a managed Subsystem from starting or
  continuing to serve its own workload.
- Registration, heartbeat, and deregistration failures must be observable but
  must not silently change the managed Subsystem lifecycle.

## 3. Responsibility Boundaries

### 3.1 Textus Control Center

Textus Control Center owns:

- the registered Subsystem instance read model;
- the managed CAR catalog and its development, local-repository, and
  public-repository source snapshots;
- registration, heartbeat, deregistration, and list Operations;
- command, automatic REST, and Web projections of the same Operations;
- stale/unreachable derivation from registration and observation facts;
- operator-facing links to each Subsystem's own System Dashboard and System
  Admin pages.

Textus Control Center does not own in Phase 1:

- starting, stopping, or restarting managed server processes;
- arbitrary PID discovery or signal delivery;
- aggregation of detailed metrics, Jobs, CallTrees, or diagnostics;
- remote-host agents, scheduling, alerting, or automatic remediation.

Textus Control Center also does not treat catalog membership as process
authority. A catalog entry can identify a CAR and its preferred source, but it
does not start, stop, or inspect an arbitrary operating-system process.

### 3.2 Textus Launcher

`textus-launcher` remains the owner of a server invocation started with
`textus <artifact> server`. When Textus Control Center integration is enabled, it:

- creates a stable instance identifier for the invocation;
- reports registration metadata before or during server startup;
- reports a bounded heartbeat while the launcher invocation is alive;
- reports normal termination when the invocation exits;
- treats management-plane communication failure as a warning rather than a
  server-start failure.

### 3.3 CNCF Launcher

`cncf-launcher` provides the same integration for canonical target-first server
execution:

- `cncf server` for the current project target;
- `cncf <target> server` for an explicitly selected target.

The integration must not use deprecated `cncf dev server` state as its source
of truth. Compatibility code may remain in the launcher, but it is outside the
Textus Control Center contract.

### 3.4 Managed Subsystem

Each managed Subsystem remains authoritative for its own:

- health and runtime status;
- component/service/operation inventory;
- metrics and observability data;
- configuration and assembly diagnostics;
- application and system administration authorization.

Phase 1 records launcher-reported facts and exposes navigation URLs. It does
not copy these detailed runtime read models into Textus Control Center.

## 4. Registration Model

The Phase 1 registration contract carries the minimum facts needed to list a
launcher-started Subsystem instance:

- `instanceId`: unique identifier generated once per launcher invocation;
- `launcherKind`: `textus` or `cncf`;
- `target`: artifact, project, or canonical launcher target label;
- `artifactId`: optional stable CAR artifact identity when resolved by the
  launcher; it links a runtime instance to a managed CAR without exposing a
  command line or repository credential;
- `subsystemName`: resolved or configured Subsystem name when known;
- `subsystemVersion`: resolved artifact/SAR version when known;
- `runtimeVersion`: resolved CNCF runtime version when known;
- `baseUrl`: operator-reachable base URL;
- `hostLabel`: configured safe host label, not an unrestricted host dump;
- `startedAt`: launcher-observed invocation start time;
- `lastSeenAt`: most recent accepted registration or heartbeat time;
- `launcherState`: `starting`, `running`, or `stopped`;
- `dashboardUrl`: link to the Subsystem System Dashboard;
- `systemAdminUrl`: link to the Subsystem System Admin page.

The contract must not expose tokens, credentials, full environment variables,
or unrestricted command lines. Process identifiers, when retained for local
diagnostics, are system-admin-only metadata and are not process-control
authority.

Textus Control Center derives an operator-facing status of `starting`, `running`,
`stale`, or `stopped` from the recorded launcher state and heartbeat freshness.
The exact timeout and transition rules must be fixed in a Phase 1
design/specification before implementation.

## 5. Configuration and Security Direction

Launcher integration is opt-in and configured independently from ordinary
CNCF runtime configuration. The effective configuration must identify:

- the Textus Control Center registration endpoint;
- whether registration is enabled;
- a bounded request timeout;
- a credential reference or other admitted authentication mechanism;
- a safe host/environment label;
- the externally reachable Subsystem base URL when it cannot be derived
  safely.

Secrets must not be written into launcher state, command output, registration
records, logs, or Web pages. Registration mutation Operations require a
dedicated machine-facing authorization policy. Human list and detail surfaces
use the normal CNCF admin authorization policy.

## 6. Projection Strategy

The registry behavior is defined once as CNCF Operations and then projected to
three required surfaces:

- Command: operators can list instances and request a structured output format.
- REST: automatic REST exposes registration mutation and read Operations.
- Web UI: an operation-backed Bootstrap/Material administration page lists
  instances, status, launcher kind, target, versions, last-seen time, and links
  to the managed Subsystem.

The canonical Operation selectors and exact REST/Web routes are frozen during
Phase 1 after the Cozy scaffold establishes the generated component and service
names. Documentation must not invent a second route family outside CNCF
projection conventions.

## 7. Phase Overview

### Phase 1: Launcher-Started Subsystem Inventory

Goal: list Subsystem instances started by `textus-launcher` and
`cncf-launcher` through command, REST, and Web UI.

Scope:

- Cozy CAR bootstrap;
- shared registration protocol;
- persistent registered-instance read model;
- register, heartbeat, deregister, list, and detail Operations;
- command, REST, and Web UI projections;
- opt-in registration clients in both launchers;
- executable cross-repository integration scenarios.

Explicitly excluded:

- lifecycle control;
- detailed health/metrics aggregation;
- multi-host agents and high-availability management plane;
- alerting and remediation.

Execution ledger:

- `docs/phase/phase-1.md`
- `docs/phase/phase-1-checklist.md`

### Phase 2: Standalone Control Center Bootstrap

Goal: make local CAR management practical by automatically registering canonical
`cncf` and `textus` launcher invocations with one local Textus Control Center.

Phase 2 keeps the Phase 1 registration protocol and instance model, but makes
the standalone deployment profile a first-class operating mode. The profile
has one local management installation and one local operator, while preserving
the management scope, registration origin, instance, and lease concepts needed
by a future distributed control plane.

Scope:

- a shared management-scope and registration-origin model that does not create
  standalone-only instance types;
- a standalone assembly with local durable state and an installation-scoped
  operator subject;
- local launcher discovery/configuration under the CNCF launcher home, with a
  compatible Textus launcher resolver;
- local registration-credential bootstrap and reference without storing a
  secret in launcher configuration or projections;
- automatic loopback base-URL derivation for canonical local server launches;
- executable standalone scenarios covering startup, heartbeat, normal exit,
  Control Center absence, and local Web/REST/command projections.

Explicitly excluded:

- remote-host discovery, host agents, or cross-host reachability;
- external identity, tenant/organization administration, and operator
  account lifecycle;
- lifecycle control, detailed health/metrics aggregation, and remediation.

Execution ledger:

- `docs/phase/phase-2.md`
- `docs/phase/phase-2-checklist.md`

### Phase 3: Managed CAR Catalog and Source Management

Goal: create a local-first managed CAR catalog that joins a logical CAR with
its development directory, local repository availability, SimpleModeling.org
publication availability, and launcher-managed runtime instances.

Scope:

- a persistent logical-CAR and CAR-source model separate from
  `RegisteredSubsystem` instances;
- development-directory discovery from explicitly configured standalone roots;
- local repository catalog discovery from the configured CNCF local repository;
- per-artifact publication metadata refresh from the configured
  SimpleModeling.org CAR repository;
- explicit subscriptions for public-only CARs, because the public repository
  currently exposes artifact catalogs rather than a global enumeration index;
- optional `artifactId` launcher registration metadata and runtime-to-catalog
  linking;
- command, REST, and static Web catalog list/detail/refresh projections;
- standalone-only source configuration under the Control Center home, with a
  future provider boundary for multi-user/control-plane deployments.

The generic CNCF/Cozy work needed for a later multi-user catalog is recorded
as a source-snapshot journal contract, not implemented as a generic CAR-list
service in this phase.  See
`/Users/asami/src/dev2025/cozy/docs/journal/2026/07/managed-car-catalog-model-and-packaging-handoff-2026-07-21.md`.

Explicitly excluded:

- starting, stopping, or restarting a CAR;
- treating an unstarted CAR as stale or unhealthy;
- arbitrary process discovery;
- unauthenticated repository mutation or publication;
- remote-repository crawling or guessed artifact names.

Execution ledger:

- `docs/phase/phase-3.md`
- `docs/phase/phase-3-checklist.md`

### Phase 4: Standalone Operational Component Control

Goal: make a machine-local Control Center the durable operating panel for
managed components. Canonical Launcher `server` invocations retain local
evidence independently of Control Center availability, and the Control Center
later reconciles a safe Launcher projection before it offers any lifecycle
action.

Phase 4 adds a persistent operational-component model between the CAR catalog
and launcher invocation inventory. A development source automatically makes a
component an operating target; a repository CAR becomes an operating target
when it is first used through a launcher. Stopping an instance does not remove
its operating target. An explicit exclusion record is required to remove it
from the panel and must survive catalog refresh.

Both CNCF Launcher and Textus Launcher write the shared, Launcher-owned
`~/.cncf/launcher/server-evidence.json` schema. The file records launch kind,
identity, target, execution mode, and lifecycle timestamps before best-effort
Control Center notification. Control Center does not read the file directly;
it uses the bounded safe Launcher projection to reconcile current and historical
evidence. `cncf server` remains the current-directory public start interface;
`cncf launcher supervisor serve` is not a required user workflow.

The accepted Phase 4 contract assigns lifecycle authority to
`textus-supervisor`, embedded in standalone Control Center or placed externally
behind the same bounded contract. Both Launchers retain common evidence and
bounded notification responsibilities. Textus Control Center submits
structured requests and projects their results without scanning for or
signalling arbitrary PIDs. See `docs/phase/phase-4.md`.

Historical closure (Jul. 22, 2026), superseded by the reopened Phase acceptance:
its black-box acceptance starts
both canonical Launcher server forms before Control Center, then reconciles one
current and one normally stopped shared-evidence record through the bounded
Launcher projection. The isolated state-home option belongs only to that test;
ordinary operator commands remain `cncf server` and `textus <artifact> server`.

Current closure (Oct. 17, 2026): the canonical Phase 4 document and checklist
close the twenty accepted Steps, embedded/external Supervisor lifecycle and
panel boundary, approved Job Audit read boundary, final validation and the sole
epoch-2 comprehensive review with focused blocker closure. Current shared
workflow main is the development/validation owner; the old checkout and its
failed validation remain historical under the explicit human decision. The
original bases and repair/review/failure history and canonical nonblocking
follow-ups remain retained. This closure does not start Phase 5 or Phase 6.

### Phase 5: Summary API and First Menu Bar Launch

The [Phase 5 plan](../phase/phase-5.md) and
[checklist](../phase/phase-5-checklist.md) own CC5-01..04: existing attributable
state -> common Summary Operation/HTTP JSON -> thin Swift/SwiftUI Menu Bar ->
Open Dashboard -> actual installation/launch/login through one supported CAR
route. Normal authentication, identity, bounded refresh and unavailable semantics
are settled before callers; no private Dashboard protocol or copied monitoring
engine. Reuse Phase 4 evidence at its actual scope.

The 2026-10-18 explicit split retains Phase 5 as the first unit and inserts 5.1
before unchanged Phase 6. Both members target six expected hours (range 5..8h)
at gpt-6.1-sol / high, standard parent mode. This Phase retains focused validation,
independent review, Step acceptance and release; its Control Center complete
sbt-full-suite is deferred-not-run to Phase 5.1 under final-only. First-route
closure does not satisfy the complete original scope.

Phase 5 is CLOSED on 2026-10-19. The actual parent-bundled macOS route passed
manual Summary/API comparison and Open Dashboard browser observation, with
enabled login registration and terminal owned cleanup. CC5-S1/S2 acceptance,
the sole independent full Phase review, focused repair closure and the local
release complete the first unit. The reciprocal Phase 5.1 remains PLANNED /
NOT STARTED and owns the deferred Control Center full suite and remaining
observations/distribution form. Original failure history and Step7/Phase0 are
retained; no successor execution follows from this closure.

### Phase 5.1: Configured Operational View and Dual CAR Distribution

Split from Phase 5. The [Phase 5.1 plan](../phase/phase-5.1.md) and
[checklist](../phase/phase-5.1-checklist.md) own CC5-05..10 after Phase 5 closure:
configured runtime/named middleware and host profiles, Docker observation,
development-CAR sm-workflow/journal projection, and the other CAR distribution
model. Both parent-bundled and separate-CAR forms use the same app/API and logical
identity and require actual results.

Consume the predecessor's accepted Summary/native/build/first-form contracts,
producer revisions, actual host/profile results and deferred-validation release
handoff. Phase 5.1 owns integrated regressions, the one Control Center complete
sbt-full-suite for PHASE-5 and PHASE-5.1, independent integrated Phase review,
setup/limitations and release closure before Phase 6. Producer repositories keep
their own authority/validation; this is a serial same-repository aggregate chain.
No child execution is started by the split.

### Phase 6: Integrated Admission Console

The [Phase 6 plan](../phase/phase-6.md) owns the Human-in-the-Loop Admission Inbox
using CNCF Phase 102 for sm-workflow, GitHub Phase/Plan PRs and Git-managed BoK
editorial candidates. It consumes authoritative provider actions/results rather
than becoming a second admission authority. Phase 5.1 read-only development status
does not implement these actions or require this whole successor to finish.

### Phase 7: Subsystem Availability and Immutable Runtime Operations

The [Phase 7 plan](../phase/phase-7.md) owns CNCF Active/Standby availability,
authorized replacement/rotation, Deployment Transaction presentation and CAR
security/update posture through CNCF108/109 contracts. Control Center projects
and requests operations; it does not grant Active authority or orchestrate its
own replacement engine. This is more specific than the previous automation label.

### Phase 8: Docker Execution Boundary and Supervisor Integration

The [Phase 8 plan](../phase/phase-8.md) consumes textus-supervisor Phase 1 for
authorized Docker lifecycle through the existing request/result boundary.
Phase 5.1 observes configured Docker services; Docker control is not pulled forward.

### Phase 9: Native Host Supervisor Execution

The [Phase 9 plan](../phase/phase-9.md) adds genuine host-native requirements using
textus-supervisor Phase 2, preserving the same operational-component, lifecycle
and audit contracts. It does not turn Control Center into a host process manager.

### Retained roadmap topics and reconciliation — 2026-10-18

The former Phase 5/6/7 titles here conflicted with the detailed Phase plans.
The allocation above follows those plans and is recorded in the
[handoff](../journal/2026/10/2026-10-18-phase-5-main-route-plan-handoff.md).
It starts no Phase and claims no implementation of the following retained topics:

- Broader multi-host lifecycle, credential rotation and role-policy management
  remain future scoped work unless explicitly covered by an adopted plan. They
  are not silently assigned to the current Phase 6 Admission Console or Phase 5.
- General alerting, maintenance policy and bounded remediation remain future
  work; only the concrete availability/deployment operations in current Phase 7
  belong there. Preserve the broad intent without restoring an unlimited scope.
- Mobile/wearable presentations and native notifications remain future targets;
  they are not completion gates for the macOS read-only route.

No unrelated upstream roadmap, full device matrix or new assurance framework is
required to deliver the Phase 5 -> 5.1 sequence. Exact remaining producer gaps
are identified at the first contract/connection package and retain their own
implementation authority.

## 8. Cross-Repository Policy

Textus Control Center, `textus-launcher`, and `cncf-launcher` remain separate
repositories. Phase work must:

- document the registration protocol independently of any one implementation;
- add executable specifications in each modified repository;
- validate launcher failure isolation as well as successful registration;
- avoid extracting a shared launcher library solely to remove small duplicated
  protocol clients;
- version protocol evolution so older launchers fail or degrade explicitly.

## 9. Phase 1 Exit Boundary

Phase 1 is complete only when an operator can start representative Subsystems
through each canonical launcher path and observe both instances from the same
Textus Control Center registry through command, REST, and Web UI, with stale and normal
termination behavior covered by executable specifications.

Completion of Phase 1 does not imply that Textus Control Center can control server
lifecycle or replace the managed Subsystem's own administration surfaces.


## 10. Docker Compose Operational Profile

For the Mac mini OPS profile, long-lived Textus/CNCF services are deployed through Docker Compose, including Control Center and the initial Textus Supervisor.

This does not replace the existing Launcher registration contract. Launchers inside CAR containers continue to register, heartbeat and deregister with Control Center over the internal Docker network. Compose service DNS may be used for the Control Center registration endpoint, while the registered Subsystem `baseUrl` remains the operator-reachable canonical URL.

Responsibility split:

- Docker Compose: desired deployment topology, network, volumes, restart policy and container configuration.
- Launcher: invocation identity, evidence, registration, heartbeat and deregistration.
- Textus Supervisor: lifecycle execution authority through an execution provider.
- Control Center: operational model, health/state projection and authorized lifecycle request surface.
- Managed Subsystem: authoritative detailed runtime health/readiness and diagnostics.

Control Center must not use the Compose file as its runtime registry and must not receive direct Docker socket authority.

Phase 8 introduces the Docker execution boundary/provider. Its model must permit a later native/host Supervisor on the same physical Mac mini. The initial implementation is Docker-only; host/native process control is planned extension scope.
