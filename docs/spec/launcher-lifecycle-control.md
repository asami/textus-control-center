status = active
scope = Phase 4 Supervisor lifecycle control

# Supervisor Lifecycle Control Specification

The former launcher-private lifecycle authority is retired from the normal
standalone path. Textus Control Center consumes the CNCF
`org.goldenport.cncf.spi.supervisor.Supervisor` contract through
`SupervisorSocket`. The selected provider owns lifecycle execution and durable
deployment ownership.

## 1. Component and Service Boundary

`textus-supervisor` defines one primary `TextusSupervisor` component and one
component-owned `Supervisor` Service. The Service has exactly two lifecycle
Operations:

- `submit`, a command corresponding to `Supervisor.submit`;
- `lookup`, a query corresponding to `Supervisor.lookup`.

The Service declares a provided standard `cncf.supervisor` SPI binding. It does
not define a component-specific lifecycle socket, a parallel Textus lifecycle
API, a status-only dummy Service, or a Componentlet.

The generated component Operations and the standard SPI provider delegate to
the same application Service implementation. The Service owns executable
operation structure, validation, authorization context, CallTree and diagnostic
participation. The CNCF SPI owns provider-neutral selection and the stable
`SupervisorRequest` and `SupervisorResult` contract.

## 2. Placement

Standalone assembly includes the `textus-supervisor` CAR and installs its local
provider into Textus Control Center's `SupervisorSocket`. The lifecycle call
may remain in-process, but it crosses the same Service semantics used by other
placements.

An external placement installs a proxy provider into the same socket. The
proxy invokes the remotely placed Supervisor Service and maps its typed result
to the CNCF SPI. Control Center lifecycle request code does not change between
local and external placement.

Neither placement requires an operator to run
`cncf launcher supervisor serve`. Launchers do not host lifecycle authority.

## 3. Standalone Ownership

The standalone `textus-supervisor` deployment resolves a selected development
CAR from Control Center's configured development catalog. It validates the
project identity, CAR kind, development directory, and declared default port
before starting the canonical command in that development directory:

`cncf server`

The directory, executable command, environment, port, credentials, and process
handle remain provider-private. They never cross the CNCF Supervisor SPI.

The provider persists request/result and owned-instance facts below the
configured standalone Control Center home. Start, Stop, and Restart operate
only on child handles created and retained by that provider. After a provider
restart, persisted request results remain available, while unproven live
process ownership fails closed. The provider never discovers ownership from a
PID, command line, or occupied port.

## 4. Requests

Every request contains:

| Field | Meaning |
| --- | --- |
| `requestId` | Stable durable lifecycle request identity. |
| `idempotencyKey` | Opaque retry identity scoped by the consumer. |
| `targetId` | Opaque managed component identity. |
| `deploymentId` | Optional placement identity. |
| `action` | `start`, `stop`, or `restart`. |
| `operatorSubjectId` | Authenticated operator identity. |
| `deadlineAt` | Absolute bounded deadline. |

The request contains no filesystem locator, shell command, environment,
credential, port, or process identifier.

Control Center commits its own `queued` audit record before dispatch. A retry
uses the same request identity and idempotency key. The provider returns the
previous result rather than creating a second managed instance.

## 5. Results and Correlation

`SupervisorResult` echoes `requestId` and reports one lifecycle state, a safe
diagnostic code/message, `supervisorId`, optional `instanceId`, and
acceptance/completion times.

Control Center accepts only a matching request ID. A malformed or mismatched
response becomes a safe terminal diagnostic and cannot update another request.
A successful lifecycle response is an ownership fact, not a runtime-health
assertion.

Launcher registration, heartbeat, deregistration, and retained common evidence
remain observational inputs to the runtime projection. They do not grant
lifecycle authority. When a supervisor-started child reports the supplied
instance correlation, Control Center may join the lifecycle and observation
records without inferring a process.

## 6. Failure Isolation

Missing, unhealthy, or ambiguous Supervisor providers are structured failures.
Control Center records a safe rejected or failed lifecycle result and retains
the operational component.

Control Center unavailability, request timeout, lost response, or restart must
not stop a provider-owned server. A provider restart may retain request
evidence while rejecting new ownership-sensitive work; it must not guess that
an unrelated process is owned.

## 7. Audit and Executable Evidence

Control Center retains action, request identity, target, operator, provider
identity, instance correlation, timestamps, outcome, and safe diagnostics. It
does not retain credentials, raw environment data, or unrestricted commands.

Executable Specifications must prove:

- the Supervisor CML contains `submit` and `lookup`, a provided standard SPI,
  and no dummy status Service or Componentlet;
- generated Operations and the SPI provider use one application Service;
- idempotent retries start at most one child;
- Stop and Restart affect only provider-owned children;
- durable lookup survives provider reconstruction while stale ownership fails
  closed;
- Control Center works through `SupervisorSocket` without launcher-private
  lifecycle commands;
- registration and launcher evidence remain observational and available when a
  lifecycle request fails.
