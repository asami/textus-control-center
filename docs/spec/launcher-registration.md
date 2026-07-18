status = draft
scope = launcher-to-textus-admin inventory protocol

# Launcher Registration Specification

## 1. Scope

This specification fixes the Phase 1 behavior for registering a Subsystem
server invocation started by `textus-launcher` or `cncf-launcher`.

It specifies the semantic Operation contract. Concrete CNCF command selectors,
automatic REST paths, and Web routes are derived after the Cozy scaffold fixes
the generated component and service identifiers.

## 2. Terms

- **instance**: one launcher-owned server invocation.
- **registration**: the first accepted report for an instance.
- **heartbeat**: a later liveness report for an already registered instance.
- **deregistration**: a normal terminal report from the owning launcher.
- **receipt time**: the Textus Admin clock time at which a report is accepted.
- **derived status**: the operator-facing state calculated from the persisted
  launcher state and receipt time.

## 3. Registration Record

Every accepted registration record contains the following logical fields.

| Field | Required | Semantics |
| --- | --- | --- |
| `protocolVersion` | yes | Major version of this protocol; Phase 1 uses `1`. |
| `instanceId` | yes | High-entropy identifier generated once per launcher invocation. |
| `launcherKind` | yes | `textus` or `cncf`. Immutable for one instance. |
| `target` | yes | Safe artifact/project/target label selected by the launcher. |
| `subsystemName` | no | Resolved or configured Subsystem name when known. |
| `subsystemVersion` | no | Resolved artifact or SAR version when known. |
| `runtimeVersion` | no | Resolved CNCF runtime version when known. |
| `baseUrl` | yes | Operator-reachable base URL of the managed Subsystem. |
| `hostLabel` | yes | Operator-supplied safe host/environment label. |
| `startedAt` | yes | Invocation start time reported by the launcher. |
| `launcherState` | yes | `starting`, `running`, or `stopped`. |
| `lastSeenAt` | generated | Receipt time assigned by Textus Admin; not supplied by the launcher. |

Textus Admin also retains the authenticated `registrationPrincipalId` as
non-projected ownership metadata. It is assigned from the accepted machine
credential and is not a launcher request field.

The protocol must reject a missing required field, unsupported
`protocolVersion`, invalid URL, unknown `launcherKind`, or unknown
`launcherState` with a structured invalid-request result.

The record must not contain credential values, authorization headers, full
command lines, environment variables, raw process metadata, CallTree payloads,
or application diagnostics.

## 4. Register Operation

The register Operation accepts a registration record with
`launcherState = starting`.

- On a previously unknown `instanceId`, it persists the record, assigns
  `lastSeenAt`, and returns the current instance projection.
- On an existing `instanceId` with identical immutable identity
  (`protocolVersion`, `launcherKind`, and `target`), it is idempotent: mutable
  metadata is refreshed and `lastSeenAt` is reassigned.
- On an existing `instanceId` with different immutable identity, it fails with
  a structured state-conflict result.
- The accepted registration does not assert runtime health.

Only an authenticated launcher credential may call the register Operation. In
Phase 1, Textus Admin's built-in machine provider accepts the server-side
configuration key `textus-admin.registration.authentication.token` and assigns
the configured `textus-admin.registration.authentication.principal-id` (default
`textus-admin-launcher`). The provider grants only the
`launcher_registration` capability; it does not grant human administration
access.

## 5. Heartbeat Operation

The heartbeat Operation accepts `instanceId` plus the mutable launcher facts
from the registration record. Its `launcherState` must be `running`.

- The instance must already exist.
- The immutable identity must match the registered record.
- Textus Admin refreshes mutable metadata and assigns a new `lastSeenAt`.
- A heartbeat for a missing instance fails with a structured not-found result;
  it does not create an instance implicitly.
- A heartbeat after accepted deregistration is rejected with a structured
  state-conflict result.

Only the same authenticated machine principal that registered the instance may
call the heartbeat Operation.

## 6. Deregister Operation

The deregister Operation accepts `instanceId` and records
`launcherState = stopped`.

- It preserves the instance in the registry for Phase 1 list/detail output;
  it does not delete the record.
- It assigns a new `lastSeenAt`.
- Repeated identical deregistration is idempotent.
- Deregistration of a missing instance returns a structured not-found result.

Only the same authenticated machine principal that registered the instance may
call the deregister Operation.

## 7. Derived Status

Textus Admin calculates status using its own clock and the configured positive
stale threshold `T`.

| Persisted launcher state | Condition | Derived status |
| --- | --- | --- |
| `stopped` | any age | `stopped` |
| `starting` | `now - lastSeenAt < T` | `starting` |
| `running` | `now - lastSeenAt < T` | `running` |
| `starting` or `running` | `now - lastSeenAt >= T` | `stale` |

`lastSeenAt` is assigned only by Textus Admin. A launcher-supplied clock cannot
extend freshness. Derived status is recomputed at read time; a background job
is not required for correctness.

`running` is not a health claim. It communicates only that the registration
lease is fresh. Detailed runtime health remains a later Phase 2 concern.

## 8. List and Detail Queries

The list query returns the safe instance projection, including:

- instance identity and launcher kind;
- target, known Subsystem/runtime version metadata, and host label;
- base URL;
- derived status and `lastSeenAt`;
- derived Dashboard and System Admin navigation URLs.

The list must order results by descending `lastSeenAt`, then ascending
`instanceId`, so ties are deterministic. Detail lookup uses `instanceId` and
returns a structured not-found result when absent.

Human list/detail Operations require normal Textus Admin/CNCF administrative
authorization. Machine mutation authorization must not grant browser list or
detail access automatically.

The machine token is server-side deployment configuration, not launcher
configuration. Deployments must supply it through their secret/configuration
mechanism and must not place its value in the CAR, registry record, logs, or
Web UI. When the key is absent, the built-in provider authenticates no launcher
requests; an enabled launcher continues its target server startup while its
registration requests are rejected normally.

The Phase 1 standalone assembly supplies a distinct local CNCF operator
subject for protected inventory reads. A request with bearer authentication
material never falls back to that local subject. Production deployments replace
the local subject with a human identity provider while preserving the separate
launcher machine provider.

## 9. Projections

Command, automatic REST, and Web UI project the same list/detail Operation
response shape.

- Command supports the canonical structured output format.
- Automatic REST maps structured invalid-request, not-found, conflict, and
  authorization results through the normal CNCF HTTP projection.
- The Web UI renders only safe fields and links. It invokes the list/detail
  Operations and does not access registry persistence directly.

## 10. Launcher Failure Isolation

When integration is enabled, a launcher attempts register before server
invocation, heartbeat while the invocation is active, and deregister in its
`finally` boundary.

If endpoint resolution, authentication, transport, timeout, or Textus Admin
Operation execution fails, the launcher:

- emits a sanitized warning with no credential value;
- bounds each request by the configured timeout;
- continues server startup and preserves the server's original exit result;
- does not retain a non-daemon task that prevents process exit.

The launcher must not use deprecated `cncf dev server` state or arbitrary PID
inspection as a fallback registration source.

## 11. Compatibility

Phase 1 supports protocol major version `1` only. A request with another major
version fails explicitly. New optional fields may be introduced only in a
compatible minor protocol revision; required field or semantic changes require
a new major version.

## 12. Executable Specification Requirements

The implementation must provide Given/When/Then executable specifications for:

- first registration and idempotent repeated registration;
- immutable-identity conflict;
- accepted heartbeat and missing/stopped-instance rejection;
- idempotent deregistration;
- stale boundary at exactly `T` using a fixed clock;
- deterministic list ordering;
- secret redaction in every projection;
- command/REST/Web equivalence for list/detail;
- Textus and canonical CNCF launcher registration, normal termination, and
  management-plane outage isolation.
