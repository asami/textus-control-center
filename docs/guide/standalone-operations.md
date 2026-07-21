# Standalone Operations Guide

status = in_progress
scope = Phase 4 operator workflow

This guide describes the implemented Control Center portion of the Phase 4
operating-panel workflow. It is deliberately a standalone local-machine
workflow: the panel records and manages operating targets, while a launcher
supervisor is the only authority that may start or stop a server.

## Operating Targets

The Control Center panel shows components that are under local management:

- a CAR found in a configured development directory is added automatically;
- a repository CAR is added after accepted launcher use;
- a stopped component remains in the panel;
- removing a component from management hides it from the normal panel but does
  not stop a running server;
- restoring management re-enables its normal source and lifecycle evaluation.

Management actions are protected administrative Operations. They never inspect
or signal operating-system processes. `Remove` changes the panel's durable
management state to `excluded`; it does not stop a running server. A subsequent
catalog refresh preserves that exclusion. `Restore` makes a development source
`auto-managed`, or returns a previously used repository CAR to `adopted`.

## Lifecycle Actions

The panel exposes protected Start, Stop, and Restart request Operations now.
Each request has an idempotency key and creates a durable, safe-to-project
lifecycle audit record. Retrying the same component/action/key returns the
same request record rather than creating a second action.

The request is currently rejected with `supervisor-not-configured` or
`supervisor-protocol-unavailable` until the local launcher supervisor protocol
is installed. A rejected request does not imply that a server was stopped or
started. Once the supervisor integration is available, an accepted request will
retain the receiver identity, instance correlation, and launcher result.

The panel is not a generic process manager. It cannot control a process that
was not launched through the authorized supervisor, and it never searches for
PIDs.

## Command and REST

The generated selectors and automatic REST paths are documented in
[`command-rest.md`](command-rest.md). Use an administrative bearer token for
management and lifecycle calls. The static panel uses exactly these Operations;
it is not a separate process-control channel.

## Related Views

- The operating panel answers what is being managed, shows lifecycle-request
  history, and submits lifecycle actions.
- CAR Catalog answers where a CAR is available from (`DEV`, `LOCAL`, or
  `PUBLIC`).
- Invocation inventory answers which launcher instances have reported their
  lifecycle facts.
