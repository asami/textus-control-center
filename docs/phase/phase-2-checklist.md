# Phase 2 - Standalone Control Center Bootstrap Checklist

This checklist is the authoritative Phase 2 progress ledger. The summary
dashboard is `phase-2.md`.

## SC-01: Shared Management Model and Profile Boundaries

Status: DONE

- [x] Define management scope, registration origin, managed Subsystem instance,
  and registration lease semantics shared by standalone and control-plane use.
- [x] Define Phase 1 registration compatibility and required protocol evolution
  behavior.
- [x] Define which profile facts are projected to operators and which remain
  internal diagnostics.
- [x] Define standalone and control-plane assembly responsibilities without
  creating standalone-only instance entities.
- [x] Record the stable decisions in `docs/design` and testable behavior in
  `docs/spec` before implementation.

Acceptance evidence:

- One domain model can represent one local installation now and multiple
  authenticated management scopes later.
- Standalone remains a profile/assembly choice rather than a parallel domain.

## SC-02: Standalone Locator and Credential Bootstrap

Status: DONE

- [x] Define the machine-level Control Center locator under the CNCF launcher
  home and its configuration precedence.
- [x] Define the compatible Textus launcher resolution path for the same
  logical locator.
- [x] Define local installation identity and machine credential bootstrap,
  rotation, and loss/recovery behavior.
- [x] Define credential references that keep token values out of tracked
  launcher configuration.
- [x] Define explicit control-plane overrides for remote endpoints and external
  identity.

Acceptance evidence:

- A local user enables automatic registration once per machine, not per CAR
  launch command.
- No credential value appears in launcher configuration, logs, or projections.

## SC-03: Standalone Control Center Assembly and State

Status: DONE

- [x] Implement the standalone assembly/profile and local installation
  operator subject.
- [x] Persist local installation/bootstrap state and registry data using the
  supported standalone datastore policy.
- [x] Enforce that launcher registration is limited to the local standalone
  credential/principal.
- [x] Preserve the existing command, REST, and Web Operation projections.
- [x] Add Given/When/Then executable specifications for bootstrap, restart,
  credential mismatch, and safe projection behavior.

Acceptance evidence:

- A restart retains the intended standalone registry and installation identity.
- An untrusted registration request cannot create a local inventory record.

## SC-04: Automatic Local CNCF Launcher Registration

Status: DONE

- [x] Resolve the standalone locator automatically for canonical `cncf server`
  and `cncf <target> server` execution.
- [x] Derive a loopback base URL from the effective local server configuration
  when no external URL override is present.
- [x] Register, heartbeat, and deregister through the shared protocol without
  inline registration arguments.
- [x] Preserve startup when the locator, credential, or Control Center is
  unavailable.
- [x] Add executable specifications for configuration precedence, URL
  derivation, lifecycle reports, and failure isolation.

Acceptance evidence:

- One standalone machine-level configuration is sufficient for representative
  local CAR launches through both canonical CNCF forms.

## SC-05: Automatic Local Textus Launcher Registration

Status: OPEN

- [ ] Resolve the shared standalone locator for canonical
  `textus <artifact> server` execution.
- [ ] Derive or preserve the local base URL using the same policy as CNCF
  launcher execution.
- [ ] Register, heartbeat, and deregister through the shared protocol without
  inline registration arguments.
- [ ] Preserve startup when the local Control Center is unavailable.
- [ ] Add executable specifications equivalent to the CNCF launcher coverage.

Acceptance evidence:

- Textus and CNCF launchers produce compatible automatic registrations for the
  same standalone Control Center.

## SC-06: Standalone Acceptance and Profile Separation

Status: OPEN

- [ ] Start a standalone Textus Control Center and representative CARs through
  both launcher types using only the installed machine-level locator.
- [ ] Verify the same records through command, REST, and Web UI.
- [ ] Verify heartbeat freshness, normal termination, and unavailable-Control
  Center startup isolation.
- [ ] Verify that credentials and internal profile diagnostics are absent from
  all projections.
- [ ] Verify that a control-plane configuration override requires explicit
  endpoint and identity configuration rather than silently using standalone
  assumptions.
- [ ] Run relevant tests in Textus Control Center, `textus-launcher`, and
  `cncf-launcher`, then complete final review and CAR lint.

Acceptance evidence:

- Local CAR management is repeatable without per-command registration setup.
- The standalone demonstration does not claim remote-host or lifecycle-control
  capability.
