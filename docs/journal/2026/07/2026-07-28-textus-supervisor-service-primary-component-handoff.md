# Textus Supervisor Service and Primary Component Handoff

Date: 2026-07-28

Status: accepted and implemented for standalone placement

Source: Textus Control Center Phase 4 lifecycle authority integration

Owner: `textus-supervisor`

Dependencies: Cozy CML generation and CNCF standard Supervisor SPI binding

## Context

Textus Control Center delegates lifecycle authority to the CNCF
`org.goldenport.cncf.spi.supervisor.Supervisor` contract. The standard SPI
defines the provider-neutral request boundary:

- `submit(SupervisorRequest): SupervisorResult`;
- `lookup(requestId): Option[SupervisorResult]`.

An SPI is not a replacement for a component Service. It is the runtime
selection and binding surface used to install a local Service implementation or
a proxy for the same Service. The Service and its Operations remain the source
of executable component behavior, protocol metadata, authorization,
observability, and transport projection.

The first standalone integration embeds the provider in Textus Control Center,
but Phase 4 also requires the same lifecycle authority to support an
independently placed provider. A model that publishes only an in-process Scala
SPI object and defines no Service cannot establish that remote operation
boundary without a separate handwritten protocol.

## Decision

`textus-supervisor` must define the real Supervisor Service and its Operations
in CML, then bind that Service as a provider of the CNCF standard Supervisor
SPI.

The component has one primary runtime participant. It does not declare or
generate a `COMPONENTLET`.

Standalone embedding and external placement are assembly choices for the same
primary component and Service. The word `embedded` does not identify a
Componentlet.

## Required CML Boundary

The CML model must contain:

- one `COMPONENT` named `TextusSupervisor` with its stable package and
  component identity;
- one component-owned Supervisor Service;
- one command Operation corresponding to `Supervisor.submit`;
- one query Operation corresponding to `Supervisor.lookup`;
- a provided standard-SPI binding for the CNCF Supervisor contract;
- no component-specific SPI socket or parallel Textus lifecycle API;
- no `COMPONENTLET` declaration.

The intended shape is:

```text
# COMPONENT

## TextusSupervisor

### SERVICE

#### Supervisor

- spi-standard :: cncf.supervisor
- spi-direction :: provides
- spi-socket :: false

# SERVICE

## Supervisor

### OPERATION

#### submit

- type :: COMMAND
- input :: SupervisorRequest
- output :: SupervisorResult

#### lookup

- type :: QUERY
- input :: SupervisorLookup
- output :: SupervisorLookupResult
```

The exact CML syntax for referencing CNCF-owned external request and result
types may require a Cozy contract extension. That extension must preserve the
CNCF types as the standard SPI authority; it must not regenerate competing
Textus-owned copies of `SupervisorRequest`, `SupervisorResult`,
`SupervisorAction`, or `SupervisorState`.

`SupervisorLookup` and `SupervisorLookupResult` above describe the Service
Operation payload shape. Their final representation may use an admitted
external-type projection or explicit component-local request/result Values, but
must preserve the exact `Supervisor.lookup` semantics.

## Service and SPI Relationship

The required execution path is:

```text
Supervisor Service / submit + lookup Operations
  -> generated Component protocol and OperationCall boundary
  -> textus-supervisor lifecycle implementation
  -> CNCF Supervisor SPI provider adapter
  -> SupervisorSocket selected by the consuming assembly
```

The Service owns:

- executable Operation structure;
- request and result validation;
- component authorization and protected administrative access;
- `ExecutionContext`, CallTree, diagnostics, and audit participation;
- local and remote transport projection;
- delegation to lifecycle logic, persistence, and deployment drivers.

The CNCF Supervisor SPI owns:

- the provider-neutral consumer contract;
- provider discovery, selection, and installation;
- the stable `SupervisorRequest` and `SupervisorResult` types;
- local provider or remote proxy substitution without changing the consumer.

The SPI provider adapter must call the component Service implementation. It
must not become a second independent implementation of lifecycle semantics.

## Primary Component Boundary

`TextusSupervisorPrimaryComponent` is the single runtime component participant
and publishes the Supervisor SPI provider.

No runtime `ComponentletFactory` is required. If CAR packaging continues to use
a `Component.BundleFactory` entrypoint, its componentlet factory set must remain
empty. The CML model must not invent a metadata-only Componentlet to describe
the primary provider or its deployment placement.

Standalone, Compose, and Kubernetes drivers are infrastructure adapters owned
behind the Supervisor Service. They are not Componentlets merely because they
are replaceable. A future Componentlet is admissible only if it represents a
real separately constructed participant containing domain logic and is backed
by a corresponding runtime `ComponentletFactory`.

## Declarations to Remove

The current generation-trigger declarations are not part of the accepted
domain boundary and must be removed:

- `SupervisorStatus` Service;
- `getSupervisorStatus` Operation;
- `GetSupervisorStatus` query payload;
- `SupervisorIdentity` Value;
- `GetSupervisorStatusResult` Value;
- `embedded-supervisor` Componentlet membership and definition.

Removing these declarations must not remove the generated
`TextusSupervisorComponent` root after the real Supervisor Service is present.

## Placement Contract

### Standalone

Textus Control Center installs the local `textus-supervisor` provider through
its `SupervisorSocket`. Calls may stay in-process, but they must pass through
the same Supervisor Service semantics and lifecycle implementation used by any
other placement.

### External

An external placement supplies a `Supervisor` proxy provider to the same
consumer socket. The proxy invokes the remotely placed Supervisor Service
Operations and maps their typed results back to the CNCF SPI contract.

External placement must not introduce a Textus Control Center-specific
lifecycle endpoint or bypass generated Operation authorization,
`ExecutionContext`, CallTree, and diagnostics.

## Acceptance Evidence

The handoff is complete only when Executable Specifications prove:

1. the CML contains the real Supervisor Service with `submit` and `lookup`;
2. the generated component protocol contains those Operations;
3. the component declares a provided CNCF Supervisor standard-SPI binding
   without a component-specific socket;
4. `TextusSupervisorPrimaryComponent` publishes the SPI provider and the
   provider delegates to the Service implementation;
5. the CML and runtime bundle contain no Componentlet;
6. the synthetic status Service, query, identity Value, and result Value are
   absent;
7. Textus Control Center can submit and look up lifecycle requests through an
   embedded provider;
8. the same Control Center consumer contract can use a remote proxy provider
   without changing its lifecycle request code;
9. authorization, `ExecutionContext`, CallTree, safe diagnostics, idempotency,
   and durable ownership behavior remain effective through both placements;
10. Start, Stop, and Restart behavior remains represented by
    `SupervisorRequest.action` and is not duplicated as a second public API.

## Out of Scope

- Adding a parallel Textus-specific lifecycle Service or socket.
- Treating a descriptive status query as the lifecycle authority.
- Modeling standalone placement as a Componentlet.
- Treating local, Compose, or Kubernetes infrastructure drivers as
  Componentlets without a real participant boundary.
- Moving `SupervisorRequest`, `SupervisorResult`, or lifecycle action/state
  ownership out of CNCF.
- Defining the network transport product or deployment topology for the remote
  proxy beyond the requirement that it use the same Supervisor Service.

## Downstream Handoff

After the `textus-supervisor` CML, generated Service, and provider adapter are
available, Textus Control Center should:

1. retain `SupervisorSocket` as its provider-neutral dependency;
2. install the local provider for standalone assembly;
3. admit a remote proxy provider for external placement;
4. run the same protected lifecycle Operations and Phase 4 acceptance behavior
   against both placements;
5. keep launcher evidence observational and best effort, without returning
   lifecycle authority to either Launcher.

## Implementation Evidence

The standalone implementation now provides:

- a CML `Supervisor` Service with generated `submit` and `lookup` Operations;
- a provided CNCF `cncf.supervisor` binding with no component-specific socket;
- one `TextusSupervisorPrimaryComponent` and an empty Componentlet factory set;
- no `SupervisorStatus`, `getSupervisorStatus`, status payload, or
  `embedded-supervisor` declaration;
- one application Service instance shared by generated OperationCalls and the
  CNCF Supervisor SPI provider;
- a Control Center assembly binding from `SupervisorSocket` to the local
  `textus-supervisor` provider;
- standalone durable request/idempotency/ownership behavior without
  launcher-private supervisor hosting or `supervisor.yaml`.

External proxy placement and launcher common-evidence notification remain
separate Phase 4 work.
