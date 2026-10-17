# Phase 9: Native Host Supervisor Execution

Status: PLANNED
Planned: future
Depends on: Phase 8
Upstream: textus-supervisor Phase 2

## Goal

Integrate the native host execution boundary provided by textus-supervisor Phase 2 so Control Center can observe and, where explicitly authorized, control native host services without moving Textus Control Center out of Docker.

## Scope direction

- Run a host/native Textus Supervisor as a separate execution boundary.
- Register Supervisor identity, host identity, capabilities and connectivity with Control Center.
- Support explicitly managed native CAR/process lifecycle through typed Supervisor operations.
- Add host-service observation for services that cannot or should not run in Docker.
- Keep runtime-instance lifecycle authority unique to one Supervisor.
- Preserve the same Control Center lifecycle request/result contract used by the Docker Supervisor.

## Boundaries

- Do not perform arbitrary PID discovery/control.
- Do not make Control Center a host agent.
- Do not grant broad host automation merely for convenience.
- launchd integration, if required, must be explicit and capability-bounded.
- Docker remains the default Mac mini OPS deployment boundary; this phase is an extension for genuine host-native requirements.

## Acceptance direction

Control Center running in Docker can show both Docker and native execution boundaries, distinguish their capabilities, observe a native managed service, and issue an authorized lifecycle request through the owning host Supervisor without direct host-process authority.
