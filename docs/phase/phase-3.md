# Phase 3 - Managed CAR Catalog and Source Management

Stage Status:

- Current status: IN PROGRESS
- Current step: Build the static Web CAR Catalog navigation, list, and
  protected detail surface from the persisted catalog Operations.
- Owner: Textus Control Center Phase 3
- Update rule: Update this block and `phase-3-checklist.md` whenever a stable
  checklist state changes. Close the phase only when every in-scope checklist
  item is complete or explicitly relocated.

status = in-progress

## 1. Purpose

Phase 3 makes Textus Control Center a practical local CAR management surface.
It adds a logical CAR catalog that records a CAR independently from whether a
server is currently running. The catalog joins explicitly configured development
directories, locally available CAR archives, and SimpleModeling.org publication
metadata with the launcher-managed runtime instances established in Phases 1
and 2.

The outcome is one operator-facing view where a CAR can be known as a local
development project, a local packaged artifact, a public published artifact,
or several of those sources at once. An unstarted CAR remains `not-running`;
only a launcher instance can become `stale`.

## 2. Scope

- Add persistent logical-CAR and CAR-source records separate from
  `RegisteredSubsystem`.
- Discover development CARs from explicit standalone configuration roots and
  their `project.yaml` descriptors.
- Read local CAR publication metadata from the configured CNCF local repository
  catalog.
- Refresh the artifact-specific public CAR catalog at
  `https://www.simplemodeling.org/repository/catalog/car/<artifactId>.yaml`.
- Seed the standalone catalog with the first-level `textus-*` CAR projects
  under the configured development root; exclude fixtures and nested examples
  unless explicitly subscribed.
- Support explicit subscriptions for public-only CARs because the public
  repository has no global artifact-listing contract.
- Extend canonical launcher registration with optional `artifactId` metadata
  so runtime instances link to the correct logical CAR.
- Provide equivalent command, automatic REST, and static Web catalog list,
  detail, and administrator-triggered refresh Operations.
- Add a CAR Catalog navigation surface without replacing the existing
  Subsystem Inventory instance view.

## 3. Boundaries

- A managed CAR is not an invocation and has no heartbeat, lease, or derived
  runtime status.
- A source refresh must not start a server, build a CAR, install dependencies,
  publish an artifact, or mutate a source repository.
- List/detail Operations read persisted source snapshots; they do not perform
  filesystem scans or outbound HTTP requests.
- Refresh is administrator-authorized, bounded by source-specific timeouts,
  and retains the last successful snapshot when a source is unavailable.
- Development directories and local filesystem locators are standalone/private
  administrative data. They appear only on protected detail projections.
- Public catalog metadata is fetched only from configured repositories and
  explicit artifact IDs; the implementation must not scrape directory listings
  or guess remote artifact names.
- Phase 3 does not add lifecycle control, arbitrary process discovery, remote
  host agents, health aggregation, or repository publication authority.

## 4. Target Model

`ManagedCar` is the logical management identity. Its stable key is
`artifactId`, not a directory name, launcher target label, or version.

`ManagedCarSource` is one availability record for a `ManagedCar`:

| Source kind | Locator | Version facts | Private detail |
| --- | --- | --- | --- |
| `development` | configured project root / `project.yaml` | project/component version and default port when known | development directory |
| `local-repository` | configured local catalog | recommended/latest local archive version | local catalog/archive path |
| `public-repository` | configured repository + artifact ID | recommended/latest stable publication and runtime requirements | public catalog URL |

`RegisteredSubsystem` remains an invocation record. It gains optional
`artifactId`; the Control Center derives runtime links by this field and only
uses legacy target/component-name matching for existing records that predate
the field.

## 5. Standalone Source Configuration

Standalone configuration lives outside the CAR, under the Control Center home,
and defines development roots, local catalog roots, public repositories, and
public-only artifact subscriptions. The initial profile uses the local
`src/dev2026` root, `~/.cncf/local/repository/catalog/car`, and the
SimpleModeling.org public CAR repository.

The configuration is an assembly concern. A later multi-user deployment
replaces its filesystem/network adapter with a provider while preserving the
same logical-CAR and source contracts.

## 6. Active Work Stack

- A (DONE): MC-01 - Freeze catalog identity, source, privacy, and runtime link
  contracts.
- B (DONE): MC-02 - Define standalone source configuration and safe
  discovery/refresh behavior.
- C (DONE): MC-03 - Generate and implement catalog records and Operations.
- D (DONE): MC-04 - Add optional launcher artifact identity and executable
  cross-launcher linkage scenarios.
- E (IN PROGRESS): MC-05 - Add command, REST, and static Web CAR catalog views.
- F (PLANNED): MC-06 - Seed representative development/local/public CARs and
  complete standalone acceptance coverage.

## 7. Development Items

- [x] MC-01: Freeze catalog and source contracts.
- [x] MC-02: Define standalone source configuration and refresh behavior.
- [x] MC-03: Implement persistent catalog records and Operations.
- [x] MC-04: Add launcher artifact identity and runtime linking.
- [ ] MC-05: Implement static Web CAR catalog projections.
- [ ] MC-06: Validate development, local, public, failure, and legacy-instance
  scenarios and close the phase.

Detailed task tracking and acceptance evidence are in
`phase-3-checklist.md`.
