# Phase 4 - Standalone Operational Component Control Checklist

This checklist is the authoritative Phase 4 progress ledger. The summary
dashboard is `phase-4.md`.

## OC-01: Operating-Target Contract

Status: DONE

- [x] Define `OperationalComponent` as a durable record keyed by `artifactId`.
- [x] Define `auto-managed`, `adopted`, and `excluded` management states.
- [x] Define runtime state independently as `not-running`, `starting`,
  `running`, `stale`, or `stopped`.
- [x] Define development-discovery auto-management, launcher-use adoption, and
  explicit exclusion precedence.
- [x] Define source-disappearance and legacy-registration behavior.

Acceptance evidence:

- A stopped component remains an operating target.
- An excluded development component does not return after catalog refresh.
- A repository CAR is adopted only after accepted use evidence exists.

## OC-02: Launch Profile and Preflight Contract

Status: DONE

- [x] Define the development-directory, local-CAR, and public-CAR launch
  profile variants.
- [x] Define declared default-port resolution and port-conflict behavior.
- [x] Define source presence, dependency, and runtime compatibility preflight.
- [x] Define the standalone single selected deployment constraint.
- [x] Ensure profiles never project credentials, unrestricted command lines, or
  private paths outside protected detail.

Acceptance evidence:

- A lifecycle request fails structurally before launch when no safe profile is
  available.
- A dynamic or conflicting port is never guessed.

## OC-03: Launcher Supervisor Lifecycle Protocol

Status: DONE

- [x] Define Start, Stop, and Restart request/result schemas, request identity,
  idempotency, timeout, and audit facts.
- [x] Define CNCF launcher local-supervisor ownership and Textus launcher
  delegation.
- [x] Define registration correlation from a successful launch request to the
  launcher-created instance identity.
- [x] Define Control Center restart, launcher restart, and timeout recovery.
- [x] Prohibit arbitrary PID discovery and signal delivery.

Acceptance evidence:

- Retrying a request cannot create a second server instance.
- Control Center loss cannot cause a launcher-owned server to stop.

## OC-04: Control Center Model and Projections

Status: IN_PROGRESS

- [x] Add the executable management-state decision model for automatic
  management, repository adoption, retained source loss, and exclusion
  precedence.
- [x] Add CML records for `OperationalComponent` and `LifecycleRequest`.
- [x] Generate protected list, detail, remove, and restore Operations for
  operating-target management; cover development auto-management, exclusion,
  restore, and launcher-use adoption with executable specifications.
- [x] Generate Start, Stop, Restart, request-status, idempotency, and audit
  Operations for lifecycle-request management.
- [x] Persist safe rejected preflights and retry-stable lifecycle audit records
  when the local supervisor is unavailable or unsupported.
- [ ] Dispatch accepted requests to the launcher supervisor and retain its
  result, receiver identity, and instance correlation.
- [ ] Implement complete administrative authorization and audit projections
  across accepted supervisor results.
- [x] Implement generated command, automatic REST, and static Web
  read/write projections through the same Operations; supervisor-unavailable
  actions retain a safe rejected audit result.
- [ ] Preserve all Phase 1–3 list, detail, source, and invocation contracts.

## OC-05: Launcher Implementations

Status: IN_PROGRESS

- [x] Implement the launcher-private supervisor endpoint, explicit
  `~/.cncf/launcher/supervisor.yaml` development-directory profile resolution,
  durable request/result state, idempotency lookup, and executable
  specifications in `cncf-launcher`.
- [x] Restrict Start, Stop, and Restart to supervisor-owned child handles;
  reject duplicate, stale-after-restart, dead-child, and persistence-failed
  ownership paths without PID discovery or arbitrary process signalling.
- [x] Implement the canonical development-directory executor and default-port
  preflight in `cncf-launcher`, including fail-closed durable-state handling.
- [x] Implement a locally hosted/configured loopback supervisor daemon in
  `cncf-launcher`, including strict private configuration, token-env lookup,
  foreground lifecycle, and safe startup failure handling.
- [ ] Implement registration/heartbeat correlation in `cncf-launcher`.
- [ ] Implement the compatible Textus launcher adapter and executable
  specifications in `textus-launcher`.
- [ ] Validate request failure isolation and registration/heartbeat continuity.

## OC-06: Operating Panel and Documentation

Status: IN_PROGRESS

- [x] Replace the top-page invocation-first layout with an
  operational-component-first panel backed by protected Operations.
- [x] Show management state, source, runtime state, active URL, last lifecycle
  result, and actions; development locators remain available only in protected
  detail projections.
- [ ] Keep invocation history and private launch detail behind protected detail
  views.
- [x] Update the command/REST reference after selectors are generated.
- [x] Publish the current standalone operator guide and phase evidence,
  including the supervisor-integration boundary.

## OC-07: Standalone Acceptance

Status: PLANNED

- [ ] Verify development discovery, exclusion, restore, and refresh behavior.
- [ ] Verify first-use adoption for repository CARs.
- [ ] Verify start, stop, restart, idempotent retry, timeout, and port-conflict
  behavior.
- [ ] Verify Control Center and launcher restart recovery.
- [ ] Run Control Center, CNCF launcher, Textus launcher, CAR packaging, and
  static Web suites.
