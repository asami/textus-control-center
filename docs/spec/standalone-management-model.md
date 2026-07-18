status = draft
scope = standalone and control-plane shared management model

# Standalone Management Model Specification

## 1. Scope Invariants

- Every accepted managed Subsystem instance belongs to exactly one management
  scope.
- A scope has a stable `scopeId`, profile, and installation identity.
- `standalone` and `control-plane` are profiles of one model, not separate
  instance schemas.
- A scope stores launcher facts; it does not obtain process-control authority
  from a PID, host label, or registration origin.

## 2. Registration Origin

An accepted registration has one origin. Phase 2 accepts only:

| Origin | Meaning |
| --- | --- |
| `cncf-launcher` | Canonical local CNCF launcher execution. |
| `textus-launcher` | Canonical local Textus launcher execution. |

An origin identifies the reporting path only. It must not grant human
administration, remote-host discovery, lifecycle control, or unrestricted host
diagnostics.

## 3. Instance and Lease Semantics

The managed Subsystem instance is identified by the Phase 1 `instanceId` and
keeps the immutable launcher identity and authenticated registration principal.

- Register creates or refreshes the instance only in the accepting scope.
- Heartbeat renews the lease only when scope, immutable identity, and
  authenticated principal match the registered instance.
- Lease freshness is derived from the accepting scope clock and a positive
  threshold.
- Deregister terminates the lease and retains a `stopped` safe projection.

## 4. Version-1 Compatibility

A Phase 1 protocol-version-1 registration remains acceptable in Phase 2.
Scope and origin are resolved by the accepting Control Center from the selected
profile and launcher integration; they are not new required request fields.

Phase 2 must reject an unsupported protocol version rather than guessing its
scope or treating arbitrary launcher data as scope authority. A future
launcher-visible scope field requires a new, documented protocol version and
explicit compatibility behavior.

## 5. Safe Projection

Command, REST, and Web projections must be derived from the same accepted
instance record. They may contain a safe scope label and origin. They must not
contain credential values, credential-reference paths, bootstrap secrets, raw
local datastore locations, full endpoint configuration, unrestricted
environment values, or process-control details.

## 6. Profile Rules

| Rule | Standalone | Control-plane |
| --- | --- | --- |
| Scope | One local installation scope | Explicitly configured authority/scope |
| Identity | Installation-scoped local operator and launcher credential | Explicit remote identity and launcher credential policy |
| Endpoint | Local locator may resolve it | Remote endpoint must be explicitly configured |
| Base URL | Loopback may be derived from effective local launch configuration | External URL policy must be explicit |

Neither profile may bypass the registration Operations or create a
profile-specific managed-instance entity.
