status = draft
scope = standalone managed CAR catalog and source snapshots

# Managed CAR Catalog Specification

## 1. Scope

This specification fixes the Phase 3 catalog contract for a standalone Textus
Control Center installation.  It covers logical CAR identity, source snapshots,
runtime correlation, protected projections, and refresh boundaries.  It does
not define server lifecycle control or a public repository enumeration API.

## 2. Terms

- **managed CAR**: a durable logical CAR record keyed by `artifactId`.
- **source snapshot**: the last successful or attempted availability result for
  one configured CAR source.
- **runtime instance**: a launcher-owned `RegisteredSubsystem` invocation.
- **not-running**: a catalog runtime presentation with no current linked
  running/starting instance; it is not an error or stale state.
- **subscription**: an explicit configured public artifact ID used to locate
  one public catalog entry.

## 3. Logical CAR Identity

`artifactId` is required and is the value declared by the CAR's
`project.name`.  It is immutable for a `ManagedCar`.

Two discoveries that resolve to the same `artifactId` update source snapshots
on the same logical CAR.  Discoveries that share only a directory name,
component name, version, archive filename, or launcher target remain distinct
until their descriptor/catalog identity proves the same `artifactId`.

A descriptor that is missing or malformed does not create a guessed CAR.  A
duplicate descriptor with the same artifact identity but conflicting component
identity/version facts records a source diagnostic and preserves the latest
unambiguous snapshot.

## 4. Source Snapshot Contract

Every `ManagedCarSource` has these logical fields:

| Field | Required | Projection rule |
| --- | --- | --- |
| `artifactId` | yes | safe |
| `sourceKind` | yes | `development`, `local-repository`, or `public-repository` |
| `sourceId` | yes | stable configuration/source identity, not a secret |
| `snapshotAt` | yes | safe |
| `refreshState` | yes | `available`, `unavailable`, or `invalid` |
| `componentName` | no | safe when supplied by source metadata |
| `componentVersion` | no | safe |
| `availableVersions` | no | safe normalized version facts |
| `diagnostic` | no | sanitized; never a credential, command line, or unrestricted path |
| `privateLocator` | no | protected administrative detail only |

The implementation may preserve source-specific facts such as recommended,
latest-stable, checksum, archive presence, and runtime requirements.  Their
presence must not make the list projection perform a live source read.

## 5. Runtime Link Contract

A runtime instance with optional `artifactId` links to a `ManagedCar` by exact
artifact identity.  Missing `artifactId` is valid for existing protocol version
1 registrations.

For missing values only, a legacy fallback may link one instance if and only if
exactly one managed CAR has an explicitly recorded matching legacy alias for
the launcher target or component identity.  It must not derive an alias from a
directory basename, archive filename, version, display text, or fuzzy match.

| Link result | Required behavior |
| --- | --- |
| direct exact artifact ID | link to that managed CAR |
| direct ID has no CAR | retain unlinked instance and emit safe catalog diagnostic |
| no ID and exactly one explicit legacy match | link as `legacy` |
| no ID and zero legacy matches | leave unlinked |
| no ID and multiple legacy matches | leave unlinked and emit ambiguity diagnostic |

`stale` remains a derived status of a linked or unlinked runtime instance.
`not-running` is derived only from the absence of a current linked instance and
never from a source-refresh failure.

## 6. Refresh and Read Operations

The catalog provides administrator-authorized list, detail, and refresh
Operations.  Command, automatic REST, and static Web projections invoke these
same Operations.

List and detail read persisted snapshots only.  Refresh reads only configured
sources with bounded source-specific timeouts:

- development roots: inspect only first-level subscribed `textus-*` project
  descriptors and exclude fixtures/nested examples unless explicitly listed;
- local repository: read the configured CNCF CAR catalog and archive-presence
  metadata;
- public repository: fetch configured subscriptions exactly; when subscriptions
  are empty, fetch the versioned `/catalog/index.json` manifest. An invalid or
  unavailable index produces the safe `public-index-invalid` or
  `public-index-unavailable` refresh diagnostic and never invents artifact IDs.

Refresh must never build, install, publish, download a CAR archive, start,
stop, restart, inspect an arbitrary PID, or crawl a directory listing.  A
failed refresh retains the last successful version facts and records an
`unavailable` or `invalid` snapshot/diagnostic.

## 7. Projection and Privacy

The catalog list shows artifact identity, source marks (`DEV`, `LOCAL`,
`PUBLIC`), selected version facts, source refresh state, and linked runtime
summaries.  A managed CAR with no running instance is visible as
`not-running`.

Development directories, local catalog/archive paths, credentials, credential
references, command lines, and raw process metadata are absent from list and
unprotected REST/Web projections.  Protected administrative detail may show a
safe local locator when authorized.  Launcher machine credentials authorize
only registration mutation and cannot read or refresh the catalog.

## 8. Compatibility

The registration protocol continues to accept Phase 1/2 records without
`artifactId`.  Adding the optional field cannot require launcher upgrades or
rewrite existing registrations.  A catalog refresh never mutates an instance
record merely to create a link.

Each source is a separate launch candidate identified exactly as
`DEV:<sourceId>`, `LOCAL:<sourceId>`, or `PUBLIC:<sourceId>`; no source
priority or fallback is permitted.

## 9. Executable Specification Requirements

Before the CML catalog model is introduced, executable specifications must
cover:

- merging development, local, and public snapshots by descriptor/catalog
  `artifactId`, never by directory basename;
- duplicate/conflicting descriptor diagnostics and deterministic ordering;
- source-refresh failure retention and list/detail no-live-I/O behavior;
- direct artifact runtime linking, compatible missing-identity reads, and
  zero/one/many legacy fallback behavior;
- `not-running` versus stale-instance semantics;
- public subscription-only retrieval, malformed public catalog handling, and
  no archive download;
- protected locator redaction across command, REST, and static Web projections.
