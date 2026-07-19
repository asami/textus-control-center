# Phase 2 - Standalone Control Center Bootstrap

Stage Status:

- Current status: IN PROGRESS
- Current step: SC-05 - Integrate automatic local `textus` launcher registration.
- Owner: Textus Control Center Phase 2
- Update rule: Update this block and `phase-2-checklist.md` whenever a stable
  checklist state changes. Close the phase only when every in-scope checklist
  item is complete or explicitly relocated.

status = in-progress

## 1. Purpose

Phase 2 makes Textus Control Center useful as a local CAR management tool. A
developer or operator starts Textus Control Center on one machine and sees
canonical `cncf` and `textus` launcher-managed CAR servers automatically,
without adding registration parameters to every launch command.

Standalone is an operating profile, not a separate domain model. The same
management scope, registration origin, managed Subsystem instance, and
registration lease concepts must support the later distributed control-plane
profile.

## 2. Scope

- Define the common standalone/control-plane management model and migration
  compatibility for Phase 1 registrations.
- Add a standalone assembly profile with one local installation, a durable
  local registry, and an installation-scoped operator subject.
- Bootstrap a local launcher-registration credential and retain only a secure
  local credential reference in launcher configuration.
- Define a common Control Center locator configuration under the CNCF launcher
  home; Textus launcher configuration must resolve the same logical locator.
- Make canonical local `cncf` and `textus` server launches register by default
  when the standalone locator is available.
- Derive safe loopback base URLs from local launch configuration when an
  external URL is not required.
- Keep launcher failure isolation: a missing or unavailable local Control
  Center never prevents a CAR server from starting.
- Provide command, REST, and Web views of the same automatically collected
  local inventory.

## 3. Boundaries

- Standalone does not discover arbitrary operating-system processes.
- Standalone does not add start, stop, restart, or signal authority.
- Credential values must not be written to tracked configuration, logs,
  registry records, command output, REST, or Web pages.
- Standalone does not emulate remote identity, tenant administration, remote
  host agents, or a distributed datastore.
- Production/control-plane configuration must remain explicit for external
  endpoints, identity, host identity, and externally reachable base URLs.
- The implementation must not introduce standalone-only Subsystem instance
  entities or bypass the existing Operation boundary.

## 4. Active Work Stack

- A (DONE): SC-01 - Freeze shared management model and profile boundaries.
- B (DONE): SC-02 - Define standalone locator and credential bootstrap.
- C (DONE): SC-03 - Implement standalone Control Center assembly/state.
- D (DONE): SC-04 - Integrate automatic local `cncf` launcher registration.
- E (IN PROGRESS): SC-05 - Integrate automatic local `textus` launcher registration.
- F (OPEN): SC-06 - Verify local inventory, failure isolation, and profile
  separation end to end.

## 5. Development Items

- [x] SC-01: Freeze the shared management model and standalone/control-plane
  boundaries.
- [x] SC-02: Define local locator, credential-reference, and configuration
  precedence contracts.
- [x] SC-03: Implement the standalone assembly and local durable state.
- [x] SC-04: Enable automatic local registration for canonical `cncf` server
  launches.
- [ ] SC-05: Enable automatic local registration for canonical `textus`
  server launches.
- [ ] SC-06: Complete executable standalone acceptance scenarios and Phase 2
  closure.

Detailed task tracking and acceptance evidence are in
`phase-2-checklist.md`.

## 6. Required Demonstration

The Phase 2 demonstration starts one local Textus Control Center and
representative local CARs through both canonical launcher paths. With only the
machine-level standalone locator installed, the instances become visible in
the Control Center command, REST, and Web projections. The demonstration also
shows that the same servers start normally when the local Control Center is
stopped or unreachable.

## 7. Completion Conditions

Phase 2 closes only when:

- standalone and future control-plane operation share one documented domain
  model and registration protocol evolution path;
- no launcher command needs an inline registration endpoint or credential;
- standalone credentials are bootstrapped and referenced without disclosure;
- local `cncf` and `textus` canonical server paths automatically register;
- derived local base URLs and registration status are deterministic;
- command, REST, and Web inventory show the same local records;
- launcher failure isolation remains covered by executable specifications;
- every item in `phase-2-checklist.md` is checked or explicitly relocated.
