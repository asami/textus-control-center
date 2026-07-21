# Operational Component Management Design

status = draft
scope = Phase 4 standalone operating targets

## Purpose

This design defines the persistent management object shown by the Textus
Control Center operating panel. It is deliberately separate from a catalog CAR
and from a launcher invocation.

## Model

`ManagedCar` remains the logical catalog identity and is keyed by `artifactId`.
`RegisteredSubsystem` remains a launcher-owned invocation fact and is keyed by
`instanceId`.

`OperationalComponent` is a durable operator-facing management record keyed by
the same `artifactId`. It has:

| Fact | Meaning |
| --- | --- |
| management state | `auto-managed`, `adopted`, or `excluded` |
| selected source | the source used by its safe local launch profile |
| runtime summary | projection of related launcher instances, not an independent health probe |
| lifecycle summary | latest requested action and its structured result |
| audit history | the accepted management and lifecycle requests |

The main panel lists only `auto-managed` and `adopted` records. `excluded`
records are retained for audit and may be restored explicitly.

## Management-State Rules

1. A successful development-source refresh creates or restores an
   `auto-managed` record unless an `excluded` record exists for the artifact.
2. An accepted launcher registration with an `artifactId` adopts that artifact
   when no development source currently manages it and it is not excluded.
3. An explicit Remove from management request sets `excluded`; it never stops
   an instance or deletes catalog, invocation, or audit facts.
4. Restore to management removes the exclusion. If a development source is
   available it becomes `auto-managed`; otherwise it becomes `adopted` only
   when previous accepted use evidence exists.
5. A source that disappears does not delete the operating target. Its selected
   source becomes unavailable and Start is disabled with a structured reason.

## Runtime Projection

Runtime state is derived only from related accepted launcher registrations and
their leases:

- `running`: a current running registration exists;
- `starting`: a current starting registration exists and no running one exists;
- `stale`: only an expired/non-terminal current registration exists;
- `stopped`: the latest relevant registration ended normally;
- `not-running`: no current invocation exists.

The operating target remains visible in every state. It is not marked stale
solely because a CAR source is missing or because no server has started.

## Lifecycle Boundary

Control Center creates a lifecycle request for the selected operational
component. A launcher-owned supervisor validates and owns the resulting child
server; it returns a structured result and normal launcher registration then
updates the runtime projection. Control Center does not use process discovery
or direct PID signalling.

For initial standalone operation, one operational component selects one local
default-port deployment. Supporting several independently placed instances of
the same artifact requires a later deployment identity.
