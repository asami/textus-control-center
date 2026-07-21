status = accepted-for-implementation
scope = Phase 4 Control Center to launcher-owned local supervisor protocol

# Launcher Lifecycle Control Specification

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

The Control Center is configured with only the loopback supervisor identity,
endpoint, timeout, and a credential *reference*. The credential value is
resolved by the Control Center's normal configuration provider and is never
stored in a lifecycle request or sent to a browser. The supervisor must bind to
loopback by default and authenticate requests before resolving a profile.

`textus-launcher` is a compatible client of this same local supervisor. It may
delegate a repository-CAR request, but it may not construct an independent
process ownership record or use a process-table search to emulate one.

## 3. Requests

The protocol admits `start`, `stop`, and `restart` requests. Every request has
an opaque request ID, an idempotency key scoped to the operational component,
the requested action, a selected launch-profile identity, an authenticated
operator identity, and a bounded timeout.

A request is sent as `POST /v1/lifecycle-requests` to the configured loopback
endpoint. It carries `Authorization: Bearer <resolved credential>` and the
following JSON values:

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

The immediate response and `GET /v1/lifecycle-requests/{requestId}` return the
same safe projection: `requestId`, `state`, `diagnosticCode`, `diagnostic`,
`supervisorId`, `instanceId`, `acceptedAt`, and `completedAt`. A lost response
is retried with the original request ID and idempotency key; the supervisor
returns the original durable record.

On a successful start or restart, the launcher supplies correlation between the
lifecycle request and its newly created `instanceId`. Ordinary registration,
heartbeat, and deregistration remain the source of runtime state; a successful
request alone does not assert that the server is healthy.

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
- an unavailable endpoint records a retry-safe `rejected` or `timed-out`
  Control Center result without affecting a server;
- Control Center restart preserves its request identity and a later lookup
  reconciles the launcher-owned result;
- launcher restart never invents ownership from a PID, command line, or port;
- registration and heartbeat remain available while a request is rejected,
  times out, or fails.
