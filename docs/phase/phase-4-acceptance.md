# Phase 4 Standalone Acceptance Evidence

status = accepted-for-review
scope = Textus Control Center Phase 4

This document maps each Phase 4 acceptance condition to executable evidence.
All tests named below passed on Jul. 22, 2026.

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

The resulting CAR is
`target/textus-control-center-0.1.0-SNAPSHOT.car`. The standalone model keeps
the Control Center as the protected operating panel and `cncf-launcher` as the
only local process owner; neither test nor implementation grants arbitrary PID
authority.
