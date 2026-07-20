# Phase 3 - Managed CAR Catalog and Source Management Checklist

This checklist is the authoritative Phase 3 progress ledger. The summary
dashboard is `phase-3.md`.

## MC-01: Catalog, Source, and Runtime-Link Contract

Status: DONE

- [x] Define `ManagedCar` stable identity as `artifactId`.
- [x] Define `ManagedCarSource` identity, source kinds, safe projected fields,
  private locator fields, and snapshot timestamps.
- [x] Define the separation of logical CAR availability from runtime instance
  state, including `not-running` versus instance `stale` semantics.
- [x] Define optional `artifactId` registration compatibility for Textus and
  CNCF launcher reports.
- [x] Define legacy runtime-link fallback and its ambiguity behavior.
- [x] Record the stable design and executable specification before CML changes.

Acceptance evidence:

- Catalog identity never depends on a development directory basename.
- Existing Phase 1/2 instance records remain readable without `artifactId`.
- No projection exposes credentials, command lines, or unrestricted local paths
  outside protected administrative detail.

## MC-02: Standalone Source Configuration and Refresh Boundary

Status: DONE

- [x] Define the standalone configuration schema and precedence for development
  roots, local catalog root, public repository, and subscriptions.
- [x] Define first-level `textus-*` seed discovery and fixture/example
  exclusion rules.
- [x] Define project descriptor validation and duplicate artifact conflict
  behavior.
- [x] Define local repository catalog parsing and archive-presence semantics.
- [x] Define artifact-specific SimpleModeling.org public catalog retrieval,
  timeout, checksum/runtime metadata, and unavailable-source behavior.
- [x] Define refresh idempotency and retention of the last successful snapshot.

Acceptance evidence:

- List/detail reads do not scan files or call remote repositories.
- Refresh never builds, starts, stops, installs, or publishes a CAR.
- Public-only CARs are explicit subscriptions until a public global manifest is
  available.

## MC-03: Cozy Catalog Model and Operations

Status: PLANNED

- [ ] Add generated CML model for logical CARs and source snapshots.
- [ ] Implement administrator-authorized refresh, list, and detail Operations.
- [ ] Preserve Phase 1/2 registration authorization boundaries.
- [ ] Implement deterministic ordering and structured source diagnostics.
- [ ] Add Given/When/Then and property-based specifications for duplicate,
  missing, stale-snapshot, and unavailable-source behavior.

Acceptance evidence:

- Command, REST, and Web use the same Operation projections.
- Source refresh failures are observable without deleting a previous snapshot.

## MC-04: Launcher Artifact Identity and Runtime Linking

Status: PLANNED

- [ ] Extend `cncf-launcher` development-directory reports with `project.name`
  as artifact identity and `project.component.name` as component identity.
- [ ] Extend `textus-launcher` artifact reports with selector artifact identity.
- [ ] Add protocol compatibility and failure-isolation specifications.
- [ ] Link current runtime instances to logical CARs by artifact identity.
- [ ] Preserve a documented legacy fallback for pre-Phase-3 registrations.

Acceptance evidence:

- A development-directory invocation identifies its component without showing a
  directory in the main inventory.
- A public artifact invocation and its development project resolve to one
  logical CAR when their artifact IDs match.

## MC-05: Command, REST, and Static Web Catalog Projections

Status: PLANNED

- [ ] Add CAR catalog list/detail projections and protected REST routes.
- [ ] Add top navigation and side-menu navigation to the static CAR Catalog
  view.
- [ ] Show source marks (`DEV`, `LOCAL`, `PUBLIC`) and selected version facts.
- [ ] Keep development directory and local paths in protected detail only.
- [ ] Show linked runtime instances separately from source availability.
- [ ] Add static Web executable coverage without external CDN dependencies.

Acceptance evidence:

- A CAR with no active instance is displayed as managed and not running, not
  stale.
- The inventory and catalog views remain independently understandable.

## MC-06: Standalone Seed and End-to-End Validation

Status: PLANNED

- [ ] Seed the configured first-level `textus-*` CAR projects under the local
  development root.
- [ ] Verify representative development-only, local-only, public-only, and
  three-source CARs.
- [ ] Verify SimpleModeling.org catalog unavailability, malformed catalog, and
  retention of the last successful snapshot.
- [ ] Verify legacy Phase 1/2 registrations and launcher outage isolation.
- [ ] Run Control Center, CNCF launcher, and Textus launcher executable suites.
- [ ] Run CAR packaging/static Web checks and close the phase.

Acceptance evidence:

- A local operator can distinguish development, local package, public release,
  and running-instance facts without reading launcher logs.
