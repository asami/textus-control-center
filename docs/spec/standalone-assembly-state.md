status = draft
scope = standalone assembly and durable registry state

# Standalone Assembly State Specification

## 1. Standalone Profile

The standalone assembly must declare:

- `textus.component.textus-control-center.datastores.application.policy` as
  `local-default`;
- one installation-scoped operator subject with operator capability; and
- the limited launcher-registration authentication provider.

## 2. Durable Registry

The registered Subsystem entity is stored in the application datastore selected
by CNCF. A newly constructed standalone runtime over the same datastore must
list registrations accepted by the prior runtime. The record continues to use
the existing registration Operation and safe projection; no direct Web or
filesystem registry is permitted.

## 3. Isolation

Executable specifications create local datastore files under `target/`. They
must not read or create a real `~/.cncf` installation. A control-plane assembly
must explicitly choose its datastore and identity configuration.
