# Phase 4 Standalone Acceptance Evidence

status = superseded-by-phase-4-evidence-integration
scope = Textus Control Center Phase 4

This document records the pre-integration executable evidence gathered on Jul.
22, 2026. Those checks remain useful regression evidence, but they do not close
Phase 4 because they accepted a foreground supervisor as a user-facing
prerequisite and did not reconcile shared Launcher evidence.

| Acceptance condition | Evidence |
| --- | --- |
| Development discovery, retained loss, exclusion, restore, refresh | `OperationalComponentManagementSpec`; `SubsystemInventoryActionSpec` |
| Repository CAR first-use adoption | `OperationalComponentManagementSpec`; `SubsystemInventoryActionSpec` |
| Start, Stop, Restart, retry, timeout, port conflict | `CncfLauncherSpec` lifecycle-supervisor scenarios; `LifecycleSupervisorProtocolSpec`; `SubsystemInventoryActionSpec` |
| Control Center and launcher recovery | `SubsystemInventoryActionSpec` standalone registry restart; `CncfLauncherSpec` durable lifecycle retry after supervisor restart |
| Safe supervisor result correlation | `LifecycleSupervisorProtocolSpec` matching and mismatched response scenarios; `CncfLauncherSpec` registration/heartbeat correlation scenario |
| CNCF launcher adapter and failure isolation | `CncfLauncherSpec` full suite, including rejected lifecycle plus registration/heartbeat continuity |
| Textus launcher adapter | `TextusLauncherSpec` full suite, including loopback submit and lookup |
| Control Center, packaging, and static Web | `sbt --batch test`, `sbt --batch cozyBuildCar`, `check-subsystem-inventory-web.sh`, `check-managed-car-catalog-web.sh` |
| Shared launcher evidence | Pending: cross-launcher evidence projection and Control Center reconciliation acceptance |

The resulting CAR at the time was
`target/textus-control-center-0.1.0-SNAPSHOT.car`. The standalone model keeps
the Control Center as the protected operating panel and Launcher as the local
evidence and lifecycle authority; neither test nor implementation grants
arbitrary PID authority.
