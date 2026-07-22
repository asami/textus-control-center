# Phase 4 Standalone Acceptance Evidence

status = accepted
scope = Textus Control Center Phase 4

This document records the executable closure evidence gathered on Jul. 22,
2026. The canonical launcher commands retain shared evidence before Control
Center availability; the later Control Center reconciles it only through the
safe Launcher projection, without a user-facing foreground supervisor
prerequisite.

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
| Shared launcher evidence | `scripts/check-phase-4-cross-launcher-acceptance.sh` is the black-box acceptance harness: it starts canonical `cncf server` and `textus <artifact> server` in one explicit isolated Launcher state home, reads their shared evidence only through `cncf launcher evidence list --format json`, and then invokes `SubsystemInventoryActionSpec` with that actual bounded command after the servers are running. It requires one current and one stopped record so Control Center proves both `current-evidence-only` and `historical-stopped` reconciliation without direct shared-file access. `--launcher-home` is test-only isolation; normal operation keeps the ordinary canonical commands. |

The resulting CAR at the time was
`target/textus-control-center-0.1.0-SNAPSHOT.car`. The standalone model keeps
the Control Center as the protected operating panel and Launcher as the local
evidence and lifecycle authority; neither test nor implementation grants
arbitrary PID authority.
