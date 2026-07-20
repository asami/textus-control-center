# Managed CAR Catalog Design

## Purpose

Phase 3 introduces a local-first catalog of CARs managed by Textus Control
Center.  It records a CAR independently from any particular launcher
invocation, then relates its known development, local-repository, and public
publication sources to launcher-managed runtime instances.

The normative behavior is specified in
`docs/spec/managed-car-catalog.md`.  This design fixes responsibility and
integration boundaries; it does not grant process or repository-mutation
authority.

## Identity and Records

`ManagedCar` is the durable logical identity for one CAR.  Its key is the
canonical `artifactId` declared by `project.name`.  A project directory name,
launcher target label, display name, component name, archive filename, and
version are not replacement keys.

`ManagedCarSource` is a persisted availability snapshot owned by one
`ManagedCar`.  It records one source kind, source configuration identity,
known version facts, refresh facts, and sanitized diagnostics.  Its source
kinds are:

- `development` for a descriptor discovered under an explicitly configured
  standalone development root;
- `local-repository` for an entry in the configured CNCF local repository
  catalog;
- `public-repository` for one configured public catalog resolved by an
  explicit artifact subscription.

`RegisteredSubsystem` remains the Phase 1/2 invocation read model.  It has
heartbeat/lease semantics and can become `stale`.  A `ManagedCar` has neither
a heartbeat nor a derived health state; when it has no linked active instance,
its runtime presentation is `not-running`.

```text
ManagedCar (artifactId)
  ├── ManagedCarSource: development
  ├── ManagedCarSource: local-repository
  ├── ManagedCarSource: public-repository
  └── RegisteredSubsystem: zero or more runtime links
```

## Source and Refresh Boundary

Discovery configuration is standalone installation data under the Control
Center home.  The CAR does not embed operator-specific source roots, local
paths, repository credentials, or public subscription policy.

Refresh performs bounded source reads and writes a new source snapshot.  List
and detail Operations read only persisted snapshots.  They must not scan a
filesystem, contact a repository, build/install/publish a CAR, or start/stop a
server.

An unavailable or malformed source retains its last successful facts and
records a new safe diagnostic.  It does not delete the logical CAR or turn a
linked runtime instance into `stale`.

The public SimpleModeling.org repository currently permits known-artifact
lookup at `repository/catalog/car/<artifactId>.yaml`, but does not offer a
global artifact index.  Phase 3 therefore refreshes only explicit public
subscriptions.  It does not guess artifact IDs or crawl remote directory
listings.

## Runtime Correlation

Canonical launcher reports gain an optional `artifactId` value.  When present,
the Control Center links an instance to the `ManagedCar` with that exact
identity.  The registration remains valid when the field is absent.

For records created before this field, the catalog may use a documented legacy
fallback only when a single managed CAR has a configured legacy alias that
exactly equals the safe launcher target or known component identity.  It must
not use a development directory basename, a version, a fuzzy display-name
match, or a guessed repository path.  Zero matches result in no link; more
than one match result in no link plus an ambiguity diagnostic.

The direct artifact link wins over every fallback.  A mismatching direct
artifact ID is a correlation diagnostic, not a reason to mutate either the
catalog record or the launcher registration.

## Privacy and Authorization

The catalog list projection contains safe identity, source marks, selected
version facts, refresh state, sanitized diagnostics, and linked runtime
summary.  It never returns development directories, local archive paths,
repository credential references, command lines, or process metadata.

Protected administrative detail can display a standalone development directory
or local catalog/archive locator only when the normal Control Center
administrative authorization policy admits it.  Machine launcher registration
authority does not imply catalog list, detail, or refresh authority.

Refresh is an administrator-authorized Operation with per-source timeouts.
It is not a general filesystem, remote-repository, process-discovery, or
lifecycle API.

## Projection Boundary

Catalog list, detail, and refresh are one Operation surface projected through
command, automatic REST, and the static Web application.  The existing
Subsystem Inventory continues to list invocation records.  The new CAR Catalog
is a separate navigation destination, so an operator can understand both a
managed-but-not-running CAR and a running instance that has not yet been added
to the catalog.

The static Web presentation uses source marks `DEV`, `LOCAL`, and `PUBLIC`.
Local paths belong only in protected detail; a source mark never implies a
running process.

## Deferred Scope

Phase 3 deliberately excludes CAR lifecycle commands, process/PID discovery,
remote-host agents, health/metric aggregation, repository mutation, archive
publication, arbitrary source crawling, and multi-user source-provider
implementation.  A later provider can replace standalone source adapters
without changing the logical CAR/source/runtime contracts.
