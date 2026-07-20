status = draft
scope = standalone managed CAR source configuration and refresh boundary

# Standalone Catalog Configuration Specification

## 1. Configuration File

The default standalone configuration file is
`<control-center-home>/catalog.yaml`.  An explicitly selected administrator or
test configuration takes precedence.  When neither is available, no source is
configured; this is a valid empty catalog, not a request to scan the machine.

Illustrative schema:

```yaml
schema: textus-control-center.catalog.v1
refresh:
  timeout: 5s
development:
  roots:
    - id: dev2026
      path: /work/src/dev2026
      include-prefix: textus-
      explicit-projects:
        - textus-control-center
local-repository:
  id: local-cncf
  catalog-root: /work/.cncf/local/repository/catalog/car
public-repositories:
  - id: simplemodeling
    catalog-base-url: https://www.simplemodeling.org/repository/catalog/car
    subscriptions:
      - textus-user-account
```

The actual parser must reject unknown schema versions and invalid values with a
safe diagnostic before adapter execution.

`scripts/configure-standalone-catalog.sh` is the standalone installation
writer. It requires an explicit absolute development root and at least one
explicit public artifact subscription, defaults the local catalog to
`<cncf-home>/local/repository/catalog/car`, writes atomically with mode `0600`,
updates the local server configuration with the Control Center home path, and
never fetches, builds, or starts a CAR.

## 2. Validation

- every source ID is non-empty and unique across development, local, and public
  source declarations;
- development and local catalog paths are absolute;
- development roots use the fixed `textus-` direct-child prefix;
- public catalog bases are HTTPS absolute URLs;
- every public repository has at least one unique explicit artifact
  subscription;
- artifact IDs contain only ASCII letters, digits, `.`, `_`, and `-`, with an
  alphanumeric first character;
- the refresh timeout is positive and at most 30 seconds.

The configuration contains no credential value.  Credential references, when a
future private repository needs them, are protected adapter-only metadata and
must never appear in catalog list projections.

## 3. Development Discovery

For a configured root, discovery evaluates only direct child paths with the
`textus-` prefix and a direct `project.yaml`.  A candidate is accepted only
when its descriptor declares `project.kind: car`, `project.name`, and
`project.component.name`.  Its stable catalog identity is the descriptor
`project.name`.

The adapter never recursively scans a root.  Test fixtures, nested examples,
and ordinary projects are ignored unless a future configuration explicitly
selects their artifact identity/project path.  Duplicate artifact IDs with
incompatible descriptor facts create a diagnostic and do not select a directory
name as a winner.

## 4. Local Repository Discovery

The adapter reads only the configured CNCF CAR catalog root.  It parses catalog
records and verifies archive-presence metadata relative to the configured
repository/catalog contract.  A missing archive is an availability diagnostic;
it does not remove the logical CAR or fabricate a version.

## 5. Public Repository Refresh

For each explicit subscription `artifactId`, the adapter requests exactly:

```text
<catalog-base-url>/<artifactId>.yaml
```

It validates schema, artifact identity, version/checksum/runtime metadata when
present, and HTTP/parse failures.  It does not enumerate the catalog base,
guess artifact IDs, or download a CAR archive.  An unavailable/malformed
response retains the last successful source facts and records a sanitized
`unavailable` or `invalid` result.

## 6. Operation Boundary

Refresh is administrator-authorized and bounded by the configured timeout.
It can read configured source inputs only.  It must not start, stop, restart,
build, install, publish, or mutate a CAR or repository.

List and detail Operations do not invoke the filesystem or HTTP adapter.  They
return persisted source snapshots.  Normal projections omit private locators;
only protected administrative detail may expose a local path.

## 7. Executable Specification Requirements

The implementation must provide Given/When/Then specifications covering:

- default versus explicit configuration precedence;
- empty configuration without host scanning;
- relative/invalid roots, duplicate IDs/subscriptions, invalid URLs, and
  timeout bounds;
- first-level `textus-*` candidate selection and fixture/nested exclusion;
- descriptor validation and duplicate artifact diagnostics;
- local archive absence and public unavailable/malformed catalog retention;
- public subscription-only URL resolution and no archive download;
- list/detail adapter isolation and private locator redaction.
