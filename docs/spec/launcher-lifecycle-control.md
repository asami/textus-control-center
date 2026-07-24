status = superseded-by-phase-4-reopen
scope = Phase 4 launcher-private lifecycle transition baseline

# Provisional Launcher Lifecycle Control Specification

This contract describes the former launcher-private authority. Reopened Phase 4
replaces it with `textus-supervisor` as the lifecycle owner. Standalone Control
Center embeds that component; future distributed placement reuses its command/
result protocol. The remaining content is retained only to bound compatibility
migration and must not be used for new lifecycle implementation.

## 1. Authority Boundary

Textus Control Center requests lifecycle work; it does not own or discover an
operating-system process. The local `cncf-launcher` supervisor owns a server it
starts and is the only standalone authority that may stop or restart it.
`textus-launcher` exposes compatible behavior by delegating to the same
supervisor contract.

No participant may fulfil a lifecycle request by searching for a PID, matching
a command line, or sending a signal to an arbitrary process.

## 2. Local Supervisor Discovery and State

`cncf-launcher` is the standalone supervisor. Its configuration and durable
ownership/request records live below the launcher-owned CNCF home (normally
`~/.cncf/`); the normal launcher configuration remains the source of runtime
and development-directory configuration. Textus Control Center must not open,
enumerate, modify, or infer those files.

Control Center invokes a bounded local `cncf launcher lifecycle` command. It
does not receive the authority endpoint, the supervisor credential, or the
authority-state location. Launcher resolves its own configuration and
environment-only credential, ensures the loopback authority when needed, then
submits or looks up the request internally.

| Control Center configuration key | Required when enabled | Meaning |
| --- | --- | --- |
| `textus-control-center.launcher.lifecycle.command` | no | One executable name; default `cncf`. Multi-token values are rejected. |
| `textus-control-center.launcher.lifecycle.timeout` | no | Bounded command and request deadline; default/minimum `20s`, maximum `30s`. This leaves authority cold-start and submission time inside one request. |

An unavailable or invalid Launcher command is recorded as a safe terminal
result. It is never replaced with direct HTTP, filesystem, PID, or credential
access by Control Center.

`textus-launcher` is a compatible client of this same local supervisor. It may
delegate a repository-CAR request, but it may not construct an independent
process ownership record or use a process-table search to emulate one.

## 3. Requests

The protocol admits `start`, `stop`, and `restart` requests. Every request has
an opaque request ID, an idempotency key scoped to the operational component,
the requested action, a selected launch-profile identity, an authenticated
operator identity, and a bounded timeout.

A request is passed through the bounded Launcher command using the following
named values:

| Field | Meaning |
| --- | --- |
| `requestId` | Control Center UUID; stable for a persisted lifecycle request. |
| `idempotencyKey` | Opaque caller key, scoped by artifact and action. |
| `artifactId` | Operational-component identity. |
| `action` | `start`, `stop`, or `restart`. |
| `operatorSubjectId` | Authenticated Control Center operator identity. |
| `deadlineAt` | Bounded absolute deadline. |

The request does not carry a filesystem locator, command line, environment,
port override, or server PID. The supervisor resolves the selected
launch-profile identity from launcher-owned local state, validates it, and
persists the request before creating or controlling a child process.

A request is accepted only when the component is managed, the launch profile
is usable, and the caller is authorized. Stop and restart additionally require
that the supervisor owns the selected active deployment. A request never grants
authority over an independently launched process.

## 4. Results and Correlation

The supervisor returns `accepted`, `running`, `stopped`, `rejected`, `failed`,
or `timed-out` with a stable diagnostic code and safe message. Repeating the
same idempotency key returns the same request/result, rather than starting an
additional server.

The Launcher `submit` response and its bounded `lookup` command return the
same safe projection: `requestId`, `state`, `diagnosticCode`, `diagnostic`,
`supervisorId`, `instanceId`, `acceptedAt`, and `completedAt`. A lost response
is retried with the original request ID and idempotency key; Launcher returns
the original durable record.

Control Center accepts a response only when its `requestId` equals the already
persisted request identity. A malformed or mismatched response is recorded as
the safe `supervisor-response-invalid` outcome and can never update another
lifecycle request's audit facts.

Control Center first commits its own request with state `queued`, then routes a
post-commit internal continuation that submits the request. The continuation is
durable work, not a browser request. If it is delayed or its response is lost,
an administrative `GetLifecycleRequest` retries the same stable request identity
and reconciles the Launcher result. An unavailable command or authority is
recorded as a safe terminal Control Center result; it never affects a
Launcher-owned server.

On a successful start or restart, the launcher supplies correlation between the
lifecycle request and its newly created `instanceId`. Ordinary registration,
heartbeat, and deregistration remain the source of runtime state; a successful
request alone does not assert that the server is healthy.

For a supervisor-created development-directory child, `cncf-launcher` allocates
the instance ID before it starts the child, stores that exact ID in the durable
lifecycle result, and passes it only as launcher-internal registration metadata.
The child launcher reuses that ID for its registration and every heartbeat; it
removes the metadata before invoking the Textus runtime. Direct launcher starts
continue to allocate a fresh instance ID. Therefore a Control Center can join a
lifecycle result and a registry record only when both report the same opaque
instance ID, without inferring a process from a port, command line, or directory.

## 5. Failure Isolation

Control Center unavailability, request timeout, lost response, or Control
Center restart must not stop a server already owned by the supervisor. The
supervisor retains sufficient local request and ownership state to answer a
safe retry. A launcher restart may report ownership as unavailable, but it must
not guess ownership from the process table.

## 6. Audit

Control Center retains requested action, request identity, target artifact,
operator, receiver identity, timestamps, outcome, and safe diagnostic. It
does not retain credentials, raw environment data, or unrestricted command
lines. The standalone protocol has one local supervisor; multi-host routing and
role policy are future extensions.

## 7. Required Executable Scenarios

- identical retries return one supervisor request and never create a second
  server instance;
- an unavailable Launcher command or authority records a retry-safe `rejected` or `timed-out`
  Control Center result without affecting a server;
- Control Center restart preserves its request identity and a later lookup
  reconciles the launcher-owned result;
- launcher restart never invents ownership from a PID, command line, or port;
- a supervisor-created child registers and heartbeats with the exact instance ID
  returned by its lifecycle result, while that internal correlation metadata is
  not forwarded to the Textus runtime;
- registration and heartbeat remain available while a request is rejected,
  times out, or fails.
