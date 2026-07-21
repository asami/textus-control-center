# Standalone Operations Guide

status = planned
scope = Phase 4 operator workflow

This guide describes the intended Phase 4 operating-panel workflow. The
operations described here are not available until Phase 4 implementation and
its generated Command/REST selectors are complete.

## Operating Targets

The Control Center panel will show components that are under local management:

- a CAR found in a configured development directory is added automatically;
- a repository CAR is added after accepted launcher use;
- a stopped component remains in the panel;
- removing a component from management hides it from the normal panel but does
  not stop a running server;
- restoring management re-enables its normal source and lifecycle evaluation.

## Lifecycle Actions

Start, Stop, and Restart will be available only when the selected component has
a usable local launch profile and the local launcher supervisor owns the
deployment where required. The panel will show accepted request status and the
subsequent launcher-reported runtime state separately.

The panel is not a generic process manager. It cannot control a process that
was not launched through the authorized supervisor, and it never searches for
PIDs.

## Related Views

- The operating panel answers what is being managed and offers lifecycle
  actions.
- CAR Catalog answers where a CAR is available from (`DEV`, `LOCAL`, or
  `PUBLIC`).
- Invocation inventory answers which launcher instances have reported their
  lifecycle facts.

The generated command and REST reference will be added to
`docs/guide/command-rest.md` only after the Operations exist.
