# Standalone Management Model

## Purpose

This design fixes the domain boundary shared by the standalone Textus Control
Center profile and the later distributed control-plane profile. Standalone is
an assembly and operating profile; it is not a second kind of managed
Subsystem or a shortcut around the existing registration Operations.

The normative behavior is in `docs/spec/standalone-management-model.md`.
The existing wire-level registration behavior remains in
`docs/spec/launcher-registration.md`.

## Domain Terms

### Management Scope

A management scope is the authority that accepts and projects registrations.
It has a stable `scopeId`, a profile, and an installation identity. A scope
does not own a launched process: it owns only accepted registration facts and
their derived operator projection.

The Phase 2 profiles are:

- `standalone`: one local installation and one local operator boundary;
- `control-plane`: an explicitly configured remote management authority.

The standalone profile has exactly one local scope. The control-plane profile
may represent multiple authenticated scopes later, but uses the same scope and
registration model.

### Registration Origin

A registration origin identifies how a registration arrived. Phase 2 admits
only `cncf-launcher` and `textus-launcher` origins. The origin is an auditable
fact, not process-control authority and not a host-discovery mechanism.

### Managed Subsystem Instance

A managed Subsystem instance is the existing registered instance record,
identified by its launcher-generated `instanceId`. It retains the Phase 1
identity, launcher state, safe metadata, authenticated registration principal,
and receipt time. It belongs to one management scope and has one registration
origin; neither fact creates a standalone-only entity type.

### Registration Lease

A registration lease begins when the scope accepts a registration and is
renewed only by an accepted heartbeat. Its expiry is derived from the scope
clock and the configured positive threshold. Deregistration terminates the
lease but retains the safe inventory record as `stopped`.

## Profile Boundaries

The standalone assembly is responsible for local installation bootstrap,
durable local state, and a local operator subject. The control-plane assembly
is responsible for explicitly configured endpoint, identity, and externally
reachable navigation policy. Neither profile changes the registration
Operation, registration ownership rule, or safe projection contract.

The standalone assembly may derive a loopback URL for a local server. It must
not infer a remote host address, scan operating-system processes, or grant
start/stop/signal authority.

## Phase 1 Compatibility

Phase 1 registration requests remain protocol version `1`. Phase 2 adds scope
and origin as server-owned context, not launcher-supplied required fields, so a
Phase 1 launcher remains valid while a standalone locator is being introduced.
When a future protocol version needs launcher-visible scope information, the
new version must be explicitly negotiated; a version-1 request must never be
silently reinterpreted as an arbitrary remote scope.

## Projection and Diagnostics

Command, REST, and Web project the same safe instance fields and derived
status. They may display a safe scope label and origin when useful to an
operator. They must not disclose credential values, credential-reference
locations, local datastore paths, raw endpoint configuration, unrestricted
environment data, or internal bootstrap diagnostics.

## Migration Direction

The current registry implementation remains the source of truth for managed
instance identity and lease state. Phase 2 extends it with common scope/origin
facts through the existing Operation path. It does not add a parallel
standalone registry, direct Web persistence, or a process supervisor.
