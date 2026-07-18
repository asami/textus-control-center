# Phase 1 - Launcher-Started Subsystem Inventory Checklist

This checklist is the authoritative Phase 1 progress ledger. The summary
dashboard is `phase-1.md`.

## TA-01: Inventory and Registration Contract

Status: DONE

- [x] Define the versioned register, heartbeat, and deregister request/response
  contract.
- [x] Define stable instance identity and duplicate/idempotent registration
  behavior.
- [x] Separate launcher-reported state, heartbeat freshness, and derived
  operator status.
- [x] Define deterministic `starting`, `running`, `stale`, and `stopped`
  transition rules.
- [x] Define the safe projected field set and system-admin-only diagnostics.
- [x] Define machine registration authentication separately from human admin
  authorization.
- [x] Define timeout, retry, and failure-isolation behavior for launcher
  clients.
- [x] Record the stable decisions in `docs/design` and testable behavior in
  `docs/spec` before implementation.

Acceptance evidence:

- A design document fixes responsibilities and invariants.
- A static specification fixes protocol fields, status derivation,
  authorization, and compatibility behavior.
- No Phase 1 behavior depends on deprecated `cncf dev server` state.

## TA-02: Cozy CAR Bootstrap

Status: DONE

- [x] Run `cozy init component` in the empty `textus-admin` repository using
  derived Textus Admin names and `0.1.0-SNAPSHOT`.
- [x] Verify `project.yaml`, `build.sbt`, `project/plugins.sbt`, and the starter
  CML.
- [x] Install `ai/directive` and root `AGENT.md`/`RULE.md` links when absent.
- [x] Replace generic starter vocabulary only through the intended CML and
  generated extension points.
- [x] Pass `sbt cozyGenerate` and `sbt compile`.
- [x] Pass initial generated tests and CNCF CAR lint.

Acceptance evidence:

- The repository has a valid Cozy CAR shape.
- Generated identifiers and public selectors match the accepted Phase 1
  naming contract.

## TA-03: Registered-Instance Model and Operations

Status: DONE

- [x] Implement the persistent registered Subsystem instance model.
- [x] Implement idempotent register behavior.
- [x] Implement heartbeat update behavior.
- [x] Implement normal deregistration/termination behavior.
- [x] Implement list and detail queries.
- [x] Implement deterministic stale-state derivation using an injected runtime
  clock.
- [x] Preserve registration attempts or structured failure evidence without
  copying secrets into persistent records.
- [x] Add Given/When/Then executable specifications and property-based coverage
  for identity, ordering, and stale-time boundaries.

Acceptance evidence:

- Operations are the single behavior boundary for every projection.
- Repeated registration and heartbeat delivery are safe.
- List ordering and status derivation are deterministic.

Evidence:

- `SubsystemRegistrySpec` covers idempotency, ownership, conflicts, stopped
  records, deterministic ordering, and generated stale-time boundaries.
- Concrete Cozy ActionCalls persist the model and project only safe operator
  fields; the internal registration principal is never returned.
- `ComponentFactorySpec` proves that `SubsystemInventory` is the sole domain
  service exposed by the component.

## TA-04: Command and REST Projections

Status: DONE

- [x] Expose list and detail through canonical CNCF command selectors.
- [x] Support canonical structured output suitable for scripts.
- [x] Expose register, heartbeat, deregister, list, and detail through automatic
  REST.
- [x] Verify REST status and structured `Conclusion` mapping for invalid,
  unauthorized, missing, and conflicting requests.
- [x] Verify command and REST return the same semantic read model.
- [x] Document command examples and REST routes generated from the accepted
  component/service/operation names.

Current evidence:

- `scripts/check-admin-read-flows.sh` verifies the generated Command selector,
  operation tree, and automatic REST routes in `TextusAdmin.meta.openapi`.
- The canonical command list selector executes with structured output; a
  deployed-CAR REST probe returns the same safe projection and preserves the
  normal structured authorization and invalid-request responses.

Acceptance evidence:

- Executable specifications compare command and REST semantics.
- No hand-written parallel REST domain path bypasses Operation dispatch.

## TA-05: Web Inventory UI

Status: DONE

- [x] Declare the Web admin page through CNCF Web packaging/descriptor
  mechanisms.
- [x] Render a responsive Bootstrap instance table.
- [x] Show status, launcher kind, target, Subsystem/runtime versions when known,
  base URL, and last-seen time.
- [x] Link to the managed Subsystem System Dashboard and System Admin pages.
- [x] Provide empty, loading, stale, stopped, and structured error states.
- [x] Enforce normal CNCF admin authorization.
- [x] Verify that Web list/detail behavior executes the same Operations as
  command and REST.
- [x] Add static Web executable coverage without external CDN
  dependencies.

Acceptance evidence:

- The CAR packages all required Web assets.
- The page contains no UI-only registry or direct persistence mutation path.

Current evidence:

- `src/main/web/textus-admin` provides the inventory table and detail dialog;
  it calls only the automatic `list-subsystems` and `get-subsystem` Operation
  routes with same-origin administrator credentials.
- `scripts/check-subsystem-inventory-web.sh` verifies the packaged-page
  contract, operation routes, protected selectors, responsive Bootstrap table,
  styling, and
  absence of external CDN dependencies.
- `sbt cozyBuildCAR` and `jar tf` verify that the CAR contains the page and
  assets. The direct classpath development server does not serve CAR static
  assets.
- A deployed-CAR browser probe renders the CNCF-launched instance with its
  status, identity, target, runtime, base URL, last-seen time, and Dashboard /
  System Admin links.

## TA-06: Textus Launcher Integration

Status: OPEN

- [ ] Define opt-in Textus Admin endpoint, timeout, credential reference, host
  label, and external base URL configuration.
- [ ] Generate one stable instance identifier per
  `textus <artifact> server` invocation.
- [ ] Send versioned registration metadata without leaking credentials or full
  environment/command-line data.
- [ ] Send bounded heartbeats while the launcher invocation is alive.
- [ ] Send normal termination/deregistration when the server invocation exits.
- [ ] Treat registration, heartbeat, and deregistration failures as observable
  warnings that do not terminate the managed server.
- [ ] Add executable specifications for success, timeout, authorization
  failure, admin outage, retry bounds, and normal termination.

Acceptance evidence:

- A representative `textus <artifact> server` invocation appears in Textus
  Admin and transitions deterministically on normal exit or heartbeat expiry.

## TA-07: CNCF Launcher Integration

Status: DONE

- [x] Define the same opt-in Textus Admin integration for canonical CNCF
  launcher configuration.
- [x] Integrate `cncf server` current-project execution.
- [x] Integrate `cncf <target> server` target-first execution.
- [x] Use the shared versioned protocol semantics without depending on
  deprecated `cncf dev server` state.
- [x] Send bounded heartbeat and normal termination notifications.
- [x] Preserve server startup when Textus Admin is unavailable or rejects the
  registration.
- [x] Add executable specifications equivalent to the Textus launcher coverage.

Acceptance evidence:

- Representative current-project and target-first server invocations appear
  in Textus Admin through the canonical launcher path.
- Launcher help and documentation teach only the canonical integration path.

Evidence:

- `CncfLauncherSpec` covers current-project and target-first lifecycles,
  bounded HTTP requests, rejected registration isolation, and query encoding.
- A deployed-CAR integration run records `running` heartbeat state and, after
  Ctrl-C, the shutdown hook records `stopped`; a pre-hook interruption expires
  to `stale` as designed.

## TA-08: Cross-Launcher Validation and Closure

Status: OPEN

- [ ] Start representative Subsystems through both canonical launchers against
  one Textus Admin server.
- [ ] Verify both instances through command, REST, and Web UI.
- [ ] Verify stable ordering and consistent fields across all projections.
- [ ] Verify normal termination and forced heartbeat-expiry behavior.
- [ ] Verify Textus Admin outage does not prevent either managed server from
  starting.
- [ ] Run full relevant tests in `textus-admin`, `textus-launcher`, and
  `cncf-launcher`.
- [ ] Run CNCF CAR lint for `textus-admin` and resolve actionable findings.
- [ ] Run final review and resolve actionable naming, specification, security,
  and protocol findings.
- [ ] Update strategy/phase status and record every deferred item in a future
  phase or dedicated deferred list.

Acceptance evidence:

- The required Phase 1 demonstration is repeatable from documented commands.
- Every Phase 1 checklist item is checked or explicitly relocated before the
  Stage Status becomes DONE or CLOSED.
