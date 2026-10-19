# Phase 8: Docker Execution Boundary and Supervisor Integration

Status: PLANNED
Planned: 2026-10-16
Depends on: [Phase 5.1](phase-5.1.md), configured Docker observation after Phase 5
Upstream: textus-supervisor Phase 1
Downstream: textus-launcher Phase 3, cncf-launcher Phase 3, development-workstation Phase 3
Related: Phase 4 standalone operational control, Phase 7 runtime operations

## Goal

Operate CNCF/Textus CARs in a Docker Compose deployment while preserving the existing Control Center, Launcher and Supervisor authority boundaries.

The first deployment target is the Mac mini OPS composition. Docker is the initial execution backend, but the operational model must permit a later native/host Supervisor without redesigning Control Center contracts.

## Architecture

```text
Mac mini
└─ Docker Compose OPS
   ├─ textus-control-center
   ├─ textus-supervisor
   ├─ CAR / Subsystem containers
   │    └─ textus-launcher or cncf-launcher
   ├─ PostgreSQL / observability services
   └─ cloudflared
```

Compose defines desired deployment topology. Control Center owns observed operational state. Textus Supervisor owns authorized lifecycle execution. Launchers retain their registration/evidence responsibilities.

## Work

1. Consume the execution-boundary/provider contract implemented by textus-supervisor Phase 1.
2. Integrate the separately deployed Docker Supervisor with the Control Center lifecycle request/result model.
3. Support authorized start, stop, restart and status projections for Supervisor-owned CAR containers.
4. Keep Docker authority out of Control Center; Control Center submits typed lifecycle requests and projects results.
5. Preserve the existing Launcher registration/heartbeat/deregistration protocol inside Docker networks.
6. Add only the minimum execution-context metadata needed to distinguish Docker execution from native execution; do not turn Launchers into Docker API clients.
7. Associate each controllable runtime instance with exactly one owning Supervisor/execution boundary.
8. Project Docker/container state separately from Launcher lease state and CNCF runtime health/readiness.
9. Allow ordinary infrastructure containers such as PostgreSQL, observability services and cloudflared to be observable without requiring CAR/Launcher semantics.
10. Keep Compose parsing out of the runtime registry. Runtime facts come from Launcher, Supervisor, CNCF operations and configured health observers.

## Supervisor model direction

The model should be capable of representing:

- `supervisorId`;
- host identity/label;
- execution boundary kind;
- supported lifecycle capabilities;
- connectivity/status;
- runtime-instance ownership.

Initial implementation:

```text
mac-mini-2026
└─ docker
   └─ textus-supervisor
```

Planned extension:

```text
mac-mini-2026
├─ docker
│  └─ textus-supervisor
└─ host
   └─ textus-supervisor
```

The host/native Supervisor is explicitly out of the initial implementation scope.

## Health/state separation

For a managed Subsystem, Control Center must be able to distinguish at least:

- deployment/container state;
- Launcher lease/registration state;
- CNCF process/runtime health and readiness;
- external endpoint reachability when configured.

A Docker `running` container is not sufficient evidence of a healthy Subsystem.

## Cloudflare boundary

`cloudflared` is deployment infrastructure, not a CAR Subcomponent requirement. It may share the Compose network and route selected public hostnames to internal services. CARs and Control Center must not contain Cloudflare-specific lifecycle logic.

## Security

- Do not mount the Docker socket into Control Center.
- Limit Docker lifecycle authority to textus-supervisor; Control Center consumes its typed protocol.
- Do not expose credentials, Compose environment secrets, Tunnel tokens or unrestricted container metadata through operational projections.
- Lifecycle requests remain authenticated/authorized Control Center operations.
- One runtime instance has one lifecycle authority even when multiple observers can report health.

## Acceptance

A Mac mini Docker OPS deployment can:

1. start Control Center and Supervisor through Compose;
2. start representative CARs using both canonical Launcher paths;
3. register and heartbeat those instances to Control Center over the Compose network;
4. show deployment, Launcher and CNCF health states independently;
5. issue an authorized lifecycle request through Control Center and execute it through the Docker Supervisor provider;
6. observe configured non-CAR infrastructure services;
7. continue to work without Control Center receiving direct Docker socket authority.

## Deferred

- native/host Supervisor execution;
- arbitrary host process/launchd control;
- multi-host scheduling;
- Kubernetes/container-orchestrator providers;
- automatic Compose mutation by Control Center;
- automatic Cloudflare configuration management.
