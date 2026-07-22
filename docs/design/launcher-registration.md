# Launcher Registration Design

## Purpose

This design defines the boundary by which `textus-launcher` and
`cncf-launcher` retain and report a server invocation to Textus Control Center.
The boundary creates an inventory of launcher-started Subsystem instances. It
does not make Textus Control Center a process supervisor.

The normative request, status, and projection behavior is defined in
`docs/spec/launcher-registration.md`.

## Responsibilities

Textus Control Center owns the durable registry read model and exposes it through
Operations. It decides the derived display status from accepted launcher facts
and server-observed receipt time.

Each launcher owns one invocation's lifecycle facts. It creates the invocation
identity and first persists start, last-seen, and normal-termination evidence in
the shared launcher store, `~/.cncf/launcher/server-evidence.json`. Both CNCF
Launcher and Textus Launcher use the same schema and serialize updates with the
same store lock. It then sends registration and heartbeat requests when a
Control Center is reachable. The launcher retains ownership of the target
process/JVM and continues server startup when Textus Control Center is
unavailable.

The Control Center does not read that local file directly. Reconciliation uses
the CNCF Launcher one-shot JSON boundary:

```text
cncf launcher evidence list --format json
```

The command reads the Launcher-owned store and returns a versioned safe
projection for records written by both CNCF Launcher and Textus Launcher. It is
not a daemon, does not start a supervisor, and does not alter the canonical
`cncf server` or `textus <artifact> server` startup experience. A later Control
Center invokes this bounded command on refresh and decides whether a record is
current or historical; thus evidence remains available even if no Control
Center was installed when `server` started.

The list projection includes identity, target/artifact, launcher kind,
execution mode, subsystem/runtime versions, and lifecycle timestamps. It never
includes tokens, command lines, environment values, process IDs, or arbitrary
host state. The development directory is omitted from list rows and may be
returned only by a protected evidence-detail projection for the local operator,
because it is required for the requested detail view but is not generally safe
to expose in a summary.

Launcher evidence is bounded local history, not an audit archive. Each shared
writer retains fresh entries for 30 days and caps the file at 512 entries.
Mutation/append order is the local recency authority: timestamps remain useful
facts but do not decide ordering across a clock adjustment. The common lock
file serializes CNCF and Textus writers; each writer atomically replaces the
JSON only after loading it. If it cannot decode the prior versioned snapshot,
it first preserves the original bytes as a uniquely named sibling recovery
file, then creates a fresh store. A failed preservation move leaves the source
untouched and the canonical server invocation continues with its existing
sanitized evidence-warning isolation.

The managed Subsystem remains the authority for its own runtime health,
metrics, Jobs, configuration, and detailed diagnostics. Phase 1 exposes links
to its System Dashboard and System Admin pages rather than copying those read
models into Textus Control Center.

## Invocation Lifecycle

For a managed server invocation, the launcher performs the following sequence:

1. Resolve the target, runtime, externally reachable base URL, and safe host
   label.
2. Create one high-entropy `instanceId` for the invocation.
3. Atomically record `startedAt`, `lastSeenAt`, and `launcherKind` in the
   launcher-owned shared evidence store.
4. Send a bounded registration request with `launcherState = starting`.
5. Start bounded daemon tasks for local evidence and, when configured, a
   Control Center heartbeat.
6. Invoke the CNCF server.
7. In a `finally` boundary, stop both tasks, record `stoppedAt`, and send a best-effort
   deregistration request with `launcherState = stopped`.

After server invocation begins, heartbeat requests report
`launcherState = running`. A launcher may omit registration/heartbeat/exit
requests when integration is disabled.

The registration client has no retry loop that can delay process shutdown or
outlive the launcher invocation. Each request uses the configured bounded
timeout. Failures are emitted as sanitized launcher warnings and do not alter
the target server's exit status. A local-evidence write failure is likewise a
sanitized warning and does not block `cncf server` or `textus <artifact> server`.

## Canonical Launcher Coverage

The Phase 1 client is installed only on canonical server execution paths:

- `textus <artifact> server`;
- `cncf server`;
- `cncf <target> server`.

Deprecated `cncf dev server`, its PID files, and its process-management
behavior are not registration sources. Client, command, help, runtime, and
server-emulator invocations do not register as server instances.

## Configuration Boundary

Both launchers expose one logical, opt-in configuration group:

```yaml
textus-control-center:
  registration:
    enabled: false
    endpoint: https://admin.example.test/rest/...
    token-env: TEXTUS_CONTROL_CENTER_REGISTRATION_TOKEN
    timeout: 2s
    heartbeat-interval: 30s
    host-label: production-a
    base-url: https://subsystem.example.test
```

The exact launcher configuration aliases and environment-prefix equivalents are
specified alongside each launcher implementation. The logical group has these
invariants:

- `enabled` defaults to `false`.
- `endpoint`, `token-env`, and `base-url` are required when enabled.
- `token-env` names an environment variable; the credential value is never
  serialized into launcher configuration, registry records, logs, or UI.
- `timeout` and `heartbeat-interval` must be positive and bounded by launcher
  policy.
- `host-label` is an operator-supplied label; a launcher does not upload a full
  host environment, command line, or unrestricted system properties.

`base-url` identifies the managed Subsystem, not Textus Control Center. Textus Control Center
derives its navigation URLs from this value using the CNCF canonical System
Dashboard and System Admin paths.

Textus Control Center holds the matching machine credential separately from this
launcher group. For Phase 1, deployment configuration supplies
`textus-control-center.registration.authentication.token` and, optionally,
`textus-control-center.registration.authentication.principal-id`. The value is an
accepted-token secret and is never part of the CAR, launcher configuration,
registry, logs, or UI. The default principal is `textus-control-center-launcher`; a
deployment can set a distinct principal ID when it provisions a distinct
credential. An absent token disables the built-in machine authentication match.

## Authority and Security

Registration mutation is machine-facing and uses a credential distinct from
human browser administration. Textus Control Center must authenticate the launcher
before it accepts register, heartbeat, or deregister requests. Human list and
detail access remains subject to the normal CNCF admin policy.

The Phase 1 provider grants only `launcher_registration`; registration Actions
require that capability in addition to an authenticated service subject. It
does not grant the administrative capability used by browser and command
inventory reads. Future IdP/user-account providers can be assembled beside it:
an unmatched bearer token is passed through instead of being treated as a
machine-token failure.

The packaged standalone assembly also declares a local CNCF operator subject
for the protected command, REST, and Web inventory reads. It is distinct from
the launcher service principal and is bypassed whenever a bearer credential is
present. A production assembly replaces this local subject with its human IdP
or user-account provider; the machine provider remains limited to registration.

Textus Control Center binds an accepted instance to the authenticated machine principal
that registered it. Later heartbeat and deregistration requests must present
that same principal; they cannot use the registry to request lifecycle control
or write credentials/diagnostic payloads. Textus Control Center must not treat a
reported PID as an authority to signal a process.

## Persistence and Status

Textus Control Center persists the latest accepted launcher facts and the server's
receipt time. `lastSeenAt` is based on the Textus Control Center clock so display status
does not rely on synchronized launcher clocks.

For Phase 1, status is a derived inventory value:

- a fresh `starting` report is `starting`;
- a fresh `running` report is `running`;
- a `stopped` report is `stopped`;
- an expired non-stopped report is `stale`.

Registration or heartbeat cannot prove application health. `running` means the
launcher is still reporting an active server invocation, not that all Subsystem
components are healthy.

## Projection Boundary

Register, heartbeat, deregister, list, and detail are Textus Control Center Operations.
Automatic REST and command use those Operations directly. The Web inventory is
an Operation-backed projection and has no separate registry/persistence path.

Public selectors and generated REST routes are established after the Cozy
scaffold fixes the component and service names. Phase 1 must not create an
unrelated hand-written REST API solely for launcher registration.

The registry persists immutable snapshots for each accepted register, heartbeat,
and deregister report. Read operations collapse these snapshots to the latest
record for each `instanceId`, so retries and heartbeat history never create
duplicate inventory rows. This avoids exposing a generic entity-update surface
for launcher-managed state.

## Deferred Scope

The following are intentionally outside this design:

- start, stop, restart, rolling deployment, or arbitrary PID control;
- remote host agents and multi-host reachability management;
- health/metrics/diagnostics aggregation;
- alerts, auto-remediation, and retention policy beyond the Phase 1 registry;
- leader election or high availability for Textus Control Center.
