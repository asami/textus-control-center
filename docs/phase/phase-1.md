# Phase 1 - Launcher-Started Subsystem Inventory

Stage Status:
- Current status: IN_PROGRESS
- Current step: TA-04 - Provide Command and REST projections.
- Owner: Textus Admin Phase 1
- Update rule: Update this block and `phase-1-checklist.md` whenever a stable
  checklist state changes. Close the phase only when every in-scope checklist
  item is complete or explicitly relocated.

status = open

## 1. Purpose

Phase 1 establishes the first usable Textus Admin capability: an operator can
list Subsystem instances started through `textus-launcher` and `cncf-launcher`
from command, REST, and Web UI.

This phase creates an inventory and discovery plane. It does not add server
start, stop, or restart control.

## 2. Scope

- Bootstrap `textus-admin` as a Cozy-generated CAR.
- Define and version a launcher-to-Textus-Admin registration protocol.
- Persist registered Subsystem instance state and heartbeat timestamps.
- Implement register, heartbeat, deregister, list, and detail Operations.
- Expose the same list/detail behavior through command, automatic REST, and an
  operation-backed Web admin page.
- Add opt-in integration to `textus-launcher` for
  `textus <artifact> server`.
- Add opt-in integration to `cncf-launcher` for `cncf server` and
  `cncf <target> server`.
- Verify normal startup, management-plane outage isolation, stale heartbeat,
  and normal launcher termination.

## 3. Boundaries

- Deprecated `cncf dev server` and its dev-server state files are not Phase 1
  integration contracts.
- Textus Admin does not discover arbitrary JVM or operating-system processes.
- Textus Admin does not send process signals or execute lifecycle commands.
- Detailed metrics, Jobs, CallTrees, configuration, and diagnostics remain
  owned by each managed Subsystem.
- Launcher registration failure must not fail the managed server startup.
- Web behavior must call the same Operations used by command and REST.
- Credentials and full command lines must not appear in registry output.

## 4. Active Work Stack

- A (DONE): TA-01 - Freeze the inventory, registration, status, and security
  contract.
- B (DONE): TA-02 - Bootstrap and validate the Cozy CAR project.
- C (DONE): TA-03 - Implement the registered-instance model and Operations.
- D (OPEN): TA-04 - Provide command and REST projections.
- E (OPEN): TA-05 - Provide the operation-backed Web inventory UI.
- F (OPEN): TA-06 - Integrate `textus-launcher` registration.
- G (OPEN): TA-07 - Integrate canonical `cncf-launcher` registration.
- H (OPEN): TA-08 - Complete cross-launcher executable scenarios and Phase 1
  closure.

## 5. Development Items

- [x] TA-01: Freeze the inventory, registration, status, and security contract.
- [x] TA-02: Bootstrap and validate the Cozy CAR project.
- [x] TA-03: Implement the registered-instance model and Operations.
- [ ] TA-04: Provide command and REST projections.
- [ ] TA-05: Provide the operation-backed Web inventory UI.
- [ ] TA-06: Integrate `textus-launcher` registration.
- [ ] TA-07: Integrate canonical `cncf-launcher` registration.
- [ ] TA-08: Complete cross-launcher validation and close Phase 1.

Detailed task tracking and acceptance evidence are in
`phase-1-checklist.md`.

## 6. Required Demonstration

The Phase 1 demonstration starts Textus Admin and representative managed
Subsystems through:

- `textus <artifact> server`;
- `cncf server` or `cncf <target> server`.

The same instances must be visible through:

- a Textus Admin command list operation;
- the corresponding automatic REST list endpoint;
- the Textus Admin Web inventory page.

The demonstration must also show that an unavailable Textus Admin endpoint
does not prevent a launcher-managed Subsystem from starting.

## 7. Completion Conditions

Phase 1 closes only when:

- the Cozy-generated CAR passes generation, compile, tests, and CAR lint;
- both launcher integrations use the versioned registration contract;
- command, REST, and Web UI expose one consistent instance read model;
- launcher kind, target, Subsystem/runtime versions when known, base URL,
  status, and last-seen time are visible;
- stale heartbeat and normal termination semantics are deterministic;
- machine registration and human administration authorization are separated;
- secrets and unrestricted command/process information are absent from all
  projections;
- representative end-to-end launcher scenarios pass;
- every item in `phase-1-checklist.md` is checked or explicitly relocated.
