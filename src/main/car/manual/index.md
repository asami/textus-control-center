# Textus Control Center Manual

Textus Control Center is the local administrative component for registered
Subsystems, managed CAR catalog facts, launcher evidence, and lifecycle request
history.

Start with the [User Guide](user-guide.md) for the supported local workflow and
diagnostic interpretation.

## Provided Services

- **SubsystemInventory** registers, refreshes, removes, lists, and retrieves
  launcher-owned Subsystem instances.
- **CarCatalog** refreshes, lists, and retrieves managed-CAR source snapshots.
- **OperationalManagement** lists and retrieves managed operational components,
  and changes their management state through remove or restore Operations.
- **LauncherEvidence** refreshes, lists, and retrieves launcher evidence
  projections.
- **LifecycleControl** accepts lifecycle requests and exposes their list and
  detail records.

The generated component contract provides the available operation selectors and
transport projections. This manual describes their operational intent, not a
separate transport surface.
