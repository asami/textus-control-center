status = draft
scope = internal development strategy

# Textus Admin Development Strategy

## 1. Purpose

Textus Admin provides an operational management subsystem for CNCF Subsystems
started through `textus-launcher` or `cncf-launcher`.

The first responsibility is cross-instance discovery. Textus Admin does not
replace the System Admin pages already hosted by each Subsystem. It provides a
management-plane index over those independently running Subsystems and links
operators to the authoritative per-Subsystem administration surfaces.

## 2. Development Principles

- Build `textus-admin` as a Cozy-generated CAR project.
- Keep launcher process ownership in the launcher; Textus Admin must not infer
  ownership from arbitrary operating-system processes.
- Use one operation model and project it to command, REST, and Web UI.
- Keep the Web UI operation-backed. It must not maintain a separate UI-only
  registry or mutate runtime state directly.
- Treat launcher registration facts, observed runtime state, and requested
  lifecycle state as different records.
- Do not depend on deprecated `cncf dev server` behavior or
  `target/cncf.d/dev-server.json`.
- Support the canonical launcher forms:
  - `textus <artifact> server`
  - `cncf server`
  - `cncf <target> server`
- A Textus Admin outage must not prevent a managed Subsystem from starting or
  continuing to serve its own workload.
- Registration, heartbeat, and deregistration failures must be observable but
  must not silently change the managed Subsystem lifecycle.

## 3. Responsibility Boundaries

### 3.1 Textus Admin

Textus Admin owns:

- the registered Subsystem instance read model;
- registration, heartbeat, deregistration, and list Operations;
- command, automatic REST, and Web projections of the same Operations;
- stale/unreachable derivation from registration and observation facts;
- operator-facing links to each Subsystem's own System Dashboard and System
  Admin pages.

Textus Admin does not own in Phase 1:

- starting, stopping, or restarting managed server processes;
- arbitrary PID discovery or signal delivery;
- aggregation of detailed metrics, Jobs, CallTrees, or diagnostics;
- remote-host agents, scheduling, alerting, or automatic remediation.

### 3.2 Textus Launcher

`textus-launcher` remains the owner of a server invocation started with
`textus <artifact> server`. When Textus Admin integration is enabled, it:

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
Textus Admin contract.

### 3.4 Managed Subsystem

Each managed Subsystem remains authoritative for its own:

- health and runtime status;
- component/service/operation inventory;
- metrics and observability data;
- configuration and assembly diagnostics;
- application and system administration authorization.

Phase 1 records launcher-reported facts and exposes navigation URLs. It does
not copy these detailed runtime read models into Textus Admin.

## 4. Registration Model

The Phase 1 registration contract carries the minimum facts needed to list a
launcher-started Subsystem instance:

- `instanceId`: unique identifier generated once per launcher invocation;
- `launcherKind`: `textus` or `cncf`;
- `target`: artifact, project, or canonical launcher target label;
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

Textus Admin derives an operator-facing status of `starting`, `running`,
`stale`, or `stopped` from the recorded launcher state and heartbeat freshness.
The exact timeout and transition rules must be fixed in a Phase 1
design/specification before implementation.

## 5. Configuration and Security Direction

Launcher integration is opt-in and configured independently from ordinary
CNCF runtime configuration. The effective configuration must identify:

- the Textus Admin registration endpoint;
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

### Phase 2: Runtime Health and Observability Summary

Goal: enrich registered instances with bounded health, version, component, and
low-cardinality runtime metric summaries obtained from stable CNCF Operations.

Phase 2 must use each Subsystem's authenticated Operation/REST boundary. It
must not scrape HTML or treat a Web dashboard JSON implementation detail as the
long-term management API.

### Phase 3: Launcher Lifecycle Control

Goal: add authorized start, stop, and restart requests through an explicit
launcher/supervisor control contract.

Textus Admin must not signal arbitrary PIDs. Lifecycle control requires a
launcher-owned or host-agent-owned authority with request identity, audit,
idempotency, timeout, and structured result semantics.

### Phase 4: Multi-Host Operations and Audit

Goal: operate registered Subsystems across hosts with explicit host identity,
credential rotation, role policy, audit history, and connectivity status.

### Phase 5: Operational Automation

Goal: add alerting, maintenance policy, rollout coordination, and bounded
automatic remediation on top of the explicit lifecycle and audit contracts.

## 8. Cross-Repository Policy

Textus Admin, `textus-launcher`, and `cncf-launcher` remain separate
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
Textus Admin registry through command, REST, and Web UI, with stale and normal
termination behavior covered by executable specifications.

Completion of Phase 1 does not imply that Textus Admin can control server
lifecycle or replace the managed Subsystem's own administration surfaces.
