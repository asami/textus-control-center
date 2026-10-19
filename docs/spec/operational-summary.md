# Operational Summary

## Operation and admission

`OperationalSummary.getSummary` is a normal CML QUERY with `GetSummary` input
and `GetSummaryResult extends OperationResult` output. Its canonical automatic
REST adapter is:

`GET /rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary?limit=50`

`limit` is optional, defaults to 50, and must be an integer from 1 through 100.
An out-of-range limit is an `operationInvalid` failure. Normal operation callers
and HTTP callers consume the same read model.

Before any metadata call or internal entity read, the caller must be
authenticated and have ApplicationContentManager, Operator, System, or Internal
as an existing privilege, capability, or role. Guests, ordinary authenticated
users, and launcher-registration credentials alone receive
`securityPermissionDenied`. This read API grants no lifecycle authority.

## Response contract

Field names are exactly camelCase. Optional fields are omitted when unavailable.
The fixed versioned business JSON uses the normal typed `OperationResponse.Json`
representation, encoded from the projected Record. This existing framework
representation preserves business names and values without the generic Record
name and locale conversion. Normal framework envelopes still wrap that same
business JSON as `data` with standard execution metadata. Domain projection,
authorization, automatic REST routing, and the business shape below are unchanged.

```json
{
  "protocolVersion": 1,
  "observedAt": "2026-10-18T00:00:00Z",
  "componentId": "<current component identifier>",
  "instanceId": "<current component instance identifier>",
  "controlCenter": {
    "status": "available",
    "version": {"state": "available", "value": "<meta.version>"},
    "uptime": {"state": "available", "seconds": 120},
    "runtimeVersion": {"state": "available", "value": "<runtime version>"}
  },
  "overallHealth": {
    "status": "ok",
    "scope": "control-center",
    "checks": [{"name": "component.reachable", "status": "ok"}]
  },
  "runtimes": {
    "total": 1, "returned": 1, "truncated": false,
    "starting": 0, "running": 1, "stopped": 0, "stale": 0,
    "health": "unavailable",
    "data": [{
      "instanceId": "<registered instance>",
      "target": "<registered target>",
      "artifactId": "<optional artifact>",
      "subsystemName": "<optional subsystem>",
      "subsystemVersion": "<optional subsystem version>",
      "runtimeVersion": "<optional runtime version>",
      "launcherState": "running", "status": "running"
    }]
  },
  "dashboardUrl": "/web/textus-control-center",
  "refreshAfterSeconds": 15
}
```

`componentId` and `instanceId` are stable printed identifiers of the actual
current component. A missing component is a structured `operationInvalid`
failure; an identity is never manufactured. `controlCenter.status` is
`available` after successful projection. `overallHealth.status` comes directly
from `Component.healthSnapshot`, the same source as CNCF `system.health`.
Its scope is only Control Center. Check records contain only name and status;
contributor details and diagnostics are excluded.

The component version comes from the normal component `meta.version` operation,
resolved through its logic and action engine using the same ExecutionContext.
An unknown or absent version has `{"state":"unavailable"}` with no value.
Runtime metadata comes only from GlobalRuntimeContext on the current public
`executionContext.scope` and its `parent` ancestry. It never uses the process-global
current context. Uptime is the nonnegative integral seconds from its actual
bootedAt to the one request-clock instant. A missing global context gives
unavailable uptime and runtimeVersion, without a fabricated zero uptime.

RegisteredSubsystem store records are read through the existing internal entity
query boundary. For each instance, the latest lastSeenAt wins, with the greatest
printed entity id breaking ties. Existing SubsystemRegistry ordering and lease
projection apply, including the 90-second stale threshold and stopped state.
Counts cover all latest instances before truncation; data contains at most
limit records in that deterministic order. `returned` is data size and
`truncated` means returned is less than total.

A fresh running lease does not establish remote runtime health: runtimes.health
is always unavailable here. Compact entries contain only the enumerated fields;
they exclude principal ids, credentials, local paths, base/application URLs,
host labels, and diagnostics. There are no per-instance calls, filesystem or
network probes, caching, persistence, or a new registry.

## Caller and source boundary

`dashboardUrl` is the canonical same-origin relative HTTP URL. Web and native
callers resolve it against the explicitly selected API server; they must not
infer a Host header or guess a port. Normal Web authentication applies. No
additional URL configuration is introduced.

Callers refresh after 15 seconds, use a five-second request timeout, and avoid
overlapping refreshes. The server creates no scheduler. Configured middleware,
host profiles, and remote observers belong to PHASE-5.1; unavailable remote
health is never aggregated as healthy or stopped, and machine names do not
classify profiles.

Executable specifications:

- [OperationalSummarySpec](../../src/test/scala/org/simplemodeling/textus/controlcenter/summary/OperationalSummarySpec.scala)
- [OperationalSummaryActionSpec](../../src/test/scala/org/simplemodeling/textus/controlcenter/OperationalSummaryActionSpec.scala)
- [ComponentFactorySpec](../../src/test/scala/org/simplemodeling/textus/controlcenter/ComponentFactorySpec.scala)

These contracts do not claim completed HTTP runtime, native, or packaged
acceptance; those require the connected parent validation.
