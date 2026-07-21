status = draft
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

## 2. Requests

The protocol admits `start`, `stop`, and `restart` requests. Every request has
an opaque request ID, an idempotency key scoped to the operational component,
the requested action, a selected launch-profile identity, an authenticated
operator identity, and a bounded timeout.

A request is accepted only when the component is managed, the launch profile
is usable, and the caller is authorized. Stop and restart additionally require
that the supervisor owns the selected active deployment. A request never grants
authority over an independently launched process.

## 3. Results and Correlation

The supervisor returns `accepted`, `running`, `stopped`, `rejected`, `failed`,
or `timed-out` with a stable diagnostic code and safe message. Repeating the
same idempotency key returns the same request/result, rather than starting an
additional server.

On a successful start or restart, the launcher supplies correlation between the
lifecycle request and its newly created `instanceId`. Ordinary registration,
heartbeat, and deregistration remain the source of runtime state; a successful
request alone does not assert that the server is healthy.

## 4. Failure Isolation

Control Center unavailability, request timeout, lost response, or Control
Center restart must not stop a server already owned by the supervisor. The
supervisor retains sufficient local request and ownership state to answer a
safe retry. A launcher restart may report ownership as unavailable, but it must
not guess ownership from the process table.

## 5. Audit

Control Center retains requested action, request identity, target artifact,
operator, receiver identity, timestamps, outcome, and safe diagnostic. It
does not retain credentials, raw environment data, or unrestricted command
lines. The standalone protocol has one local supervisor; multi-host routing and
role policy are future extensions.
