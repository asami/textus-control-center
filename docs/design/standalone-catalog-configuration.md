# Standalone Catalog Configuration Design

## Purpose

Phase 3 keeps source ownership local to one Control Center installation.  This
design defines the standalone configuration boundary for development CAR
projects, the local CNCF repository catalog, and explicitly subscribed public
CARs.  It deliberately keeps this data outside the CAR and outside launcher
registration payloads.

The normative schema and refresh rules are in
`docs/spec/standalone-catalog-configuration.md`.

## Location and Precedence

The standard standalone location is:

```text
<control-center-home>/catalog.yaml
```

where the Control Center home is the established standalone locator root.  An
explicit catalog configuration path supplied by an administrator or test
assembly takes precedence over this standard location.  If neither supplies a
configuration, the catalog is empty and the existing Subsystem Inventory
continues to work.

The CAR archive, assembly descriptor, launcher configuration, environment, and
arbitrary current working directory are not source-configuration fallbacks.
This avoids publishing one operator's local paths or treating a launcher target
as management policy.

## Source Selection

Development discovery is opt-in through absolute configured roots.  For each
root, the adapter considers only a direct child whose name starts with
`textus-` and whose direct `project.yaml` declares a CAR with a valid
`project.name` and `project.component.name`.  It does not recurse.  Fixtures,
nested examples, and non-CAR projects are excluded unless an administrator
adds a separately explicit project/artifact selection.

The local repository source reads only its configured `catalog/car` root and
the archive facts referenced by that catalog.  It must not use an arbitrary
repository root inferred from a user home or a CAR archive path.

Each public source names a configured HTTPS catalog base and a non-empty list
of artifact subscriptions.  Refresh constructs only the documented
artifact-specific catalog request for each subscription.  It does not discover
or scrape a global public listing.

## Refresh Boundary

The source configuration defines one bounded timeout, with a maximum of
30 seconds.  A refresh adapter receives the validated configuration and may
read only the roots, catalog roots, and public subscription URLs it names.

Refresh is explicit and administrator-authorized.  It produces source
snapshots and diagnostics; it does not build, install, publish, download an
archive, start/stop a process, or modify any source repository.  List/detail
Operations consume those snapshots and never invoke the adapters.

An unavailable or malformed source yields a safe diagnostic and retains the
last successful snapshot.  A malformed project descriptor never creates a CAR
from a directory basename.

## Privacy Boundary

Configuration paths, local archive paths, and any repository credential
reference are installation-private data.  The configuration contains no token
values.  Its source locators are available only to the refresh adapter and
protected administrative detail; normal list, REST, and static Web projections
receive only source marks, selected version facts, timestamps, and sanitized
diagnostics.

## Multi-User Transition

A future control-plane provider may supply the same validated source model from
a multi-user administrative store.  It must preserve the logical source IDs,
explicit public subscriptions, bounded refresh, snapshot retention, and
privacy rules.  It must not reinterpret standalone paths as tenant-wide policy.
