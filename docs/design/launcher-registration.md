# Launcher Registration Design

## Purpose

This design defines the boundary by which `textus-launcher` and
`cncf-launcher` report a server invocation to Textus Admin during Phase 1.
The boundary creates an inventory of launcher-started Subsystem instances. It
does not make Textus Admin a process supervisor.

The normative request, status, and projection behavior is defined in
`docs/spec/launcher-registration.md`.

## Responsibilities

Textus Admin owns the durable registry read model and exposes it through
Operations. It decides the derived display status from accepted launcher facts
and server-observed receipt time.

Each launcher owns one invocation's lifecycle facts. It creates the invocation
identity, sends registration and heartbeat requests, and reports normal
termination. The launcher retains ownership of the target process/JVM and
continues server startup when Textus Admin is unavailable.

The managed Subsystem remains the authority for its own runtime health,
metrics, Jobs, configuration, and detailed diagnostics. Phase 1 exposes links
to its System Dashboard and System Admin pages rather than copying those read
models into Textus Admin.

## Invocation Lifecycle

For a managed server invocation, the launcher performs the following sequence:

1. Resolve the target, runtime, externally reachable base URL, and safe host
   label.
2. Create one high-entropy `instanceId` for the invocation.
3. Send a bounded registration request with `launcherState = starting`.
4. Start a bounded daemon heartbeat task.
5. Invoke the CNCF server.
6. In a `finally` boundary, stop the heartbeat task and send a best-effort
   deregistration request with `launcherState = stopped`.

After server invocation begins, heartbeat requests report
`launcherState = running`. A launcher may omit registration/heartbeat/exit
requests when integration is disabled.

The registration client has no retry loop that can delay process shutdown or
outlive the launcher invocation. Each request uses the configured bounded
timeout. Failures are emitted as sanitized launcher warnings and do not alter
the target server's exit status.

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
textus-admin:
  registration:
    enabled: false
    endpoint: https://admin.example.test/rest/...
    token-env: TEXTUS_ADMIN_REGISTRATION_TOKEN
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

`base-url` identifies the managed Subsystem, not Textus Admin. Textus Admin
derives its navigation URLs from this value using the CNCF canonical System
Dashboard and System Admin paths.

## Authority and Security

Registration mutation is machine-facing and uses a credential distinct from
human browser administration. Textus Admin must authenticate the launcher
before it accepts register, heartbeat, or deregister requests. Human list and
detail access remains subject to the normal CNCF admin policy.

Textus Admin binds an accepted instance to the authenticated machine principal
that registered it. Later heartbeat and deregistration requests must present
that same principal; they cannot use the registry to request lifecycle control
or write credentials/diagnostic payloads. Textus Admin must not treat a
reported PID as an authority to signal a process.

## Persistence and Status

Textus Admin persists the latest accepted launcher facts and the server's
receipt time. `lastSeenAt` is based on the Textus Admin clock so display status
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

Register, heartbeat, deregister, list, and detail are Textus Admin Operations.
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
- leader election or high availability for Textus Admin.
