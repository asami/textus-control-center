# Textus Control Center User Guide

## Purpose

Use Textus Control Center to view and administer the local inventory that is
owned by the configured launcher and supervisor integration. It keeps distinct
views for registered Subsystem instances, managed CAR availability, launcher
evidence, and lifecycle-request history.

## Local Workflow

1. Use the Subsystem Inventory service to inspect the launcher-owned instance
   records that have registered or refreshed their state.
2. Use the CAR Catalog service to refresh configured sources and inspect the
   retained availability snapshots for managed CARs.
3. Use Operational Management to inspect the local management state and, when
   appropriate, remove or restore a managed component without treating the
   panel as a generic process manager.
4. Submit a Start, Stop, or Restart request through Lifecycle Control and then
   inspect the retained request record rather than inferring the outcome from a
   process search.

## Diagnostics

Launcher evidence and lifecycle requests retain safe diagnostics when a local
provider, launcher, or supervisor is temporarily unavailable. A diagnostic does
not by itself erase retained inventory, prove that a component stopped, or
grant lifecycle authority. Refresh the relevant service view after the local
condition has been corrected, then use the resulting record and its observation
time to determine the current state.

Catalog source summaries intentionally keep machine-specific locations out of
normal list views. Use the component's authorized detail projection when that
information is required for local investigation.
