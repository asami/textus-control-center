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

## OC-03: Shared Launcher Server Evidence

Status: DONE

Decision `P4-EVIDENCE-PROJECTION-01` (Jul. 22, 2026): CNCF Launcher provides
the common evidence read boundary through the one-shot
`cncf launcher evidence list --format json` command. It reads the shared store
for both launcher kinds. Control Center does not read `~/.cncf/launcher/`
directly or require a foreground supervisor/service. The list omits the local
development path; a separately protected detail projection may disclose that
path to the local operator.

- [x] Define one shared launcher-owned store at
  `~/.cncf/launcher/server-evidence.json` with a versioned schema.
- [x] Record start, last-seen, normal-stop, instance identity, target, execution
  mode, runtime, and `launcherKind` from canonical CNCF and Textus server
  invocations, independently of Control Center availability.
- [x] Serialize CNCF/Textus Launcher updates through a common local lock so one
  launcher cannot overwrite another launcher's evidence.
- [x] Define and implement the safe Launcher list/detail projection; Control Center must not
  read the shared file directly.
- [x] Define and implement bounded-refresh reconciliation from that projection to
  Control Center's invocation and operational-component decisions.
- [x] Define and implement 30-day/512-entry retention, malformed-evidence
  recovery copies, mutation-order clock-skew handling, and common-lock
  concurrent-writer preservation.

Acceptance evidence:

- `cncf server` and `textus <artifact> server` leave equivalent durable evidence
  when Control Center is unavailable.
- A later Control Center can distinguish current candidate evidence from normal
  termination without direct local-file access.

## OC-04: Control Center Evidence Reconciliation and Projections

Status: DONE

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
- [x] Dispatch accepted requests through the durable post-commit supervisor
  continuation; retain safe result, receiver identity, and instance correlation.
- [x] Implement complete administrative authorization and audit projections
  across accepted supervisor results.
- [x] Implement generated command, automatic REST, and static Web
  read/write projections through the same Operations; supervisor-unavailable
  actions retain a safe rejected audit result.
- [x] Preserve all Phase 1–3 list, detail, source, and invocation contracts.
- [x] Add protected Control Center Operations that obtain only the safe evidence
  projection from Launcher.
- [x] Reconcile evidence with registration/heartbeat facts without allowing
  evidence alone to claim process-control authority.
- [x] Project launcher kind, evidence freshness, and historical/current
  decision reasons through detail, command, REST, and the static Web panel.

## OC-05: Canonical Launcher Lifecycle Authority

Status: REOPENED

Decision `P4-TEXTUS-SUPERVISOR-01` (Jul. 24, 2026): `textus-supervisor` is the
lifecycle authority. Standalone Control Center embeds its local deployment;
future Compose/Kubernetes operation deploys the same contract independently.
`cncf server` and `textus <artifact> server` remain the only public
server-start interfaces. Launchers record common evidence and may notify the
supervisor, but they do not host lifecycle authority.

Planned implementation sequence:

1. Define the supervisor command/result protocol and explicit standalone versus
   distributed placement boundary.
2. Move durable lifecycle request, ownership, and execution responsibility to
   `textus-supervisor`; Control Center becomes its protected management client.
3. Reduce both Launchers to common-evidence persistence and bounded notification
   that never blocks a canonical server start.
4. Prove standalone embedding and an external supervisor placement through the
   same acceptance surface.

- [x] Prototype the launcher-private supervisor endpoint, explicit
  `~/.cncf/launcher/supervisor.yaml` development-directory profile resolution,
  durable request/result state, idempotency lookup, and executable
  specifications in `cncf-launcher`.
- [x] Restrict Start, Stop, and Restart to supervisor-owned child handles;
  reject duplicate, stale-after-restart, dead-child, and persistence-failed
  ownership paths without PID discovery or arbitrary process signalling.
- [x] Implement the canonical development-directory executor and default-port
  preflight in `cncf-launcher`, including fail-closed durable-state handling.
- [x] Prototype a locally hosted/configured loopback supervisor daemon in
  `cncf-launcher`, including strict private configuration, token-env lookup,
  foreground lifecycle, and safe startup failure handling.
- [x] Implement registration/heartbeat correlation in `cncf-launcher`.
- [x] Implement the compatible Textus launcher adapter and executable
  specifications in `textus-launcher`.
- [x] Validate request failure isolation and registration/heartbeat continuity.
- [x] Define a Launcher-managed Start/Stop/Restart boundary that does not require
  users to run `cncf launcher supervisor serve`.
- [x] Keep `cncf server` current-directory recognition and `textus <artifact>
  server` as the public canonical start interfaces.
- [x] Derive the standalone development launch profile from retained shared
  Launcher evidence written by canonical `cncf server`; a legacy
  `supervisor.yaml` directory mapping is only a migration fallback.
- [x] Restrict Control Center lifecycle actions to explicit Launcher-managed
  authority; never discover or signal an arbitrary process.
- [x] Define the `textus-supervisor` component identity, command/result
  protocol, ownership persistence, and standalone embedding contract.
- [x] Add the `textus-supervisor` primary component Service, generated
  `submit`/`lookup` Operations, CNCF standard SPI provider, and standalone
  assembly binding without a dummy Componentlet or status Service.
- [x] Replace launcher-private authority hosting and `supervisor.yaml` as the
  normal lifecycle authority path.
- [ ] Make CNCF Launcher and Textus Launcher write common evidence and issue
  best-effort supervisor notification without lifecycle ownership.
- [ ] Revalidate lifecycle requests, panel actions, restart recovery, and the
  unchanged canonical launcher commands against embedded and external placement.

## OC-06: Operating Panel and Documentation

Status: REVALIDATE

- [x] Replace the top-page invocation-first layout with an
  operational-component-first panel backed by protected Operations.
- [x] Show management state, source, runtime state, active URL, last lifecycle
  result, and actions; development locators remain available only in protected
  detail projections.
- [x] Keep invocation history and private launch detail behind protected detail
  views.
- [x] Update the command/REST reference after selectors are generated.
- [x] Publish the current standalone operator guide and phase evidence,
  including the Launcher-evidence boundary.
- [x] Replace the supervisor prerequisite in panel guidance and show evidence
  reconciliation status and decision reason.

## OC-07: Standalone Acceptance

Status: REOPENED

- [x] Verify development discovery, exclusion, restore, and refresh behavior.
- [x] Verify first-use adoption for repository CARs.
- [x] Verify start, stop, restart, idempotent retry, timeout, and port-conflict
  behavior.
- [x] Verify Control Center and launcher restart recovery.
- [x] Run Control Center, CNCF launcher, Textus launcher, CAR packaging, and
  static Web suites.
- [x] Verify cross-launcher shared-evidence persistence, safe projection, and
  Control Center reconciliation when the Control Center starts after servers.
- [x] Verify that normal `cncf server` remains sufficient for a development
  directory and no user-facing foreground-supervisor command is required.
- [ ] Verify the same lifecycle behavior with Control Center's embedded
  `textus-supervisor` and with an externally placed supervisor contract.
