# Development Script Prepare Separation Hygiene

Status: Open hygiene

## Finding

The server launcher consumes a cached runtime classpath, but development checks
such as `scripts/check-phase-4-cross-launcher-acceptance.sh` and
`scripts/check-control-center-read-flows.sh` invoke top-level SBT. The cached
classpath is checked only for presence; there is no aggregate prepare entry or
digest-backed freshness contract.

## Required direction

- Put build, CAR, launcher/dependency, and runtime-evidence work in one prepare
  entry routed through the shared serialized SBT runner.
- Keep runtime and HTTP/read-flow checks top-level-SBT-free.
- Validate prepared artifacts and their inputs by digest and reject stale state
  before launching.
- Avoid re-preparing unchanged artifacts during ordinary repair restarts.

## Completion evidence

The serialized prepare entry succeeds; runtime/check scripts contain no
top-level SBT; current evidence runs; missing or stale evidence fails early and
does not trigger an implicit repair.
