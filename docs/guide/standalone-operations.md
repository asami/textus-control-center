# Standalone Operations Guide

status = implemented
scope = Phase 4 operator workflow

This guide describes the Control Center portion of the Phase 4 operating-panel
workflow. It is deliberately a standalone local-machine workflow: a launcher
started with its normal `server` command records local evidence first, then the
panel presents the launcher-provided operational view. A separate
`cncf launcher supervisor serve` command is not a user-facing prerequisite.

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

The request is committed as `queued` and dispatched after commit through the
bounded local Launcher CLI. Launcher internally ensures its local authority;
the operator never starts `cncf launcher supervisor serve` as a prerequisite.
Its safe result retains the supervisor, instance correlation, outcome,
timestamps, and diagnostic in the protected component detail view. An
unavailable Launcher command or authority is retained as a safe rejected audit
result; it does not imply that a server was stopped or started.

The bounded lifecycle timeout is `20s` by default and cannot be set below that
value. It includes internal authority cold-start and request submission; it is
not a process-health assertion.

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
  audit history in protected component detail, and submits lifecycle actions.
- CAR Catalog answers where a CAR is available from (`DEV`, `LOCAL`, or
  `PUBLIC`).
- Invocation inventory answers which launcher instances have reported their
  lifecycle facts. Its source evidence is retained by CNCF Launcher and Textus
  Launcher together under `~/.cncf/launcher/server-evidence.json`, even while
  Control Center is unavailable.

On panel refresh, Control Center asks CNCF Launcher for the shared evidence
projection. It does not open that file itself. If `cncf` is temporarily absent
or the projection is unavailable, existing inventory and management rows stay
usable; the panel does not make the supervisor command a prerequisite. The
component detail view can show the development directory for a selected local
evidence record, while summaries deliberately omit it.
