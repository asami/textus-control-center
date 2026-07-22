# Control Center Command and REST Reference

The canonical Command selectors are generated from the Cozy component and
service names. They are intentionally distinct from deprecated `cncf dev
server` behavior.

| Operation | Command selector | Automatic REST path |
|---|---|---|
| Register | `textus-control-center.subsystem-inventory.register-subsystem` | `GET /rest/v1/textus-control-center/subsystem-inventory/register-subsystem` |
| Heartbeat | `textus-control-center.subsystem-inventory.heartbeat-subsystem` | `GET /rest/v1/textus-control-center/subsystem-inventory/heartbeat-subsystem` |
| Deregister | `textus-control-center.subsystem-inventory.deregister-subsystem` | `GET /rest/v1/textus-control-center/subsystem-inventory/deregister-subsystem` |
| List | `textus-control-center.subsystem-inventory.list-subsystems` | `GET /rest/v1/textus-control-center/subsystem-inventory/list-subsystems` |
| Detail | `textus-control-center.subsystem-inventory.get-subsystem` | `GET /rest/v1/textus-control-center/subsystem-inventory/get-subsystem` |

For example, an authenticated administrative command invocation is:

```sh
cncf command textus-control-center.subsystem-inventory.list-subsystems
```

The corresponding REST request is:

```sh
curl --fail-with-body \\
  -H "Authorization: Bearer $TEXTUS_ADMIN_TOKEN" \\
  'https://admin.example.test/rest/v1/textus-control-center/subsystem-inventory/list-subsystems?text=sample&offset=0&limit=100'
```

Registration operations require an authenticated launcher principal. List and
detail require an administrative principal. Missing, invalid, conflicting, and
unauthorized requests are returned as CNCF structured Conclusions; automatic
REST maps them to the corresponding HTTP status. The projected response never
contains `registrationPrincipalId`.

`TextusControlCenter.meta.openapi` is the machine-readable source of the automatic
REST paths. `scripts/check-control-center-read-flows.sh` verifies the generated
selectors and OpenAPI routes.

## Operational Management

These administrative Operations manage the durable operating-target panel.
`List` omits excluded components; use `Detail` for a known managed artifact.
`Remove` changes the component to `excluded` without stopping a running server,
and `Restore` restores its source-derived management state.

| Operation | Command selector | Automatic REST path |
|---|---|---|
| List | `textus-control-center.operational-management.list-operational-components` | `GET /rest/v1/textus-control-center/operational-management/list-operational-components` |
| Detail | `textus-control-center.operational-management.get-operational-component` | `GET /rest/v1/textus-control-center/operational-management/get-operational-component` |
| Remove | `textus-control-center.operational-management.remove-operational-component` | `GET /rest/v1/textus-control-center/operational-management/remove-operational-component` |
| Restore | `textus-control-center.operational-management.restore-operational-component` | `GET /rest/v1/textus-control-center/operational-management/restore-operational-component` |

## Launcher Evidence

These protected Operations invoke the bounded CNCF Launcher JSON projection;
they do not access `~/.cncf/launcher/` directly. List output excludes the
development directory. Detail is the local-operator path for that field.

| Operation | Command selector | Automatic REST path |
|---|---|---|
| Refresh | `textus-control-center.launcher-evidence.refresh-launcher-evidence` | `GET /rest/v1/textus-control-center/launcher-evidence/refresh-launcher-evidence` |
| List | `textus-control-center.launcher-evidence.list-launcher-evidence` | `GET /rest/v1/textus-control-center/launcher-evidence/list-launcher-evidence` |
| Detail | `textus-control-center.launcher-evidence.get-launcher-evidence` | `GET /rest/v1/textus-control-center/launcher-evidence/get-launcher-evidence` |

## Lifecycle Requests

Start, Stop, and Restart create idempotent lifecycle-request records. The same
`artifactId`, action, and `idempotencyKey` returns the original request. Until a
Launcher lifecycle authority is unavailable, these actions eventually retain a
safe `rejected` request; they do not start or stop an operating-system process.

| Operation | Command selector | Automatic REST path |
|---|---|---|
| Start | `textus-control-center.lifecycle-control.start-operational-component` | `GET /rest/v1/textus-control-center/lifecycle-control/start-operational-component` |
| Stop | `textus-control-center.lifecycle-control.stop-operational-component` | `GET /rest/v1/textus-control-center/lifecycle-control/stop-operational-component` |
| Restart | `textus-control-center.lifecycle-control.restart-operational-component` | `GET /rest/v1/textus-control-center/lifecycle-control/restart-operational-component` |
| List requests | `textus-control-center.lifecycle-control.list-lifecycle-requests` | `GET /rest/v1/textus-control-center/lifecycle-control/list-lifecycle-requests` |
| Request detail | `textus-control-center.lifecycle-control.get-lifecycle-request` | `GET /rest/v1/textus-control-center/lifecycle-control/get-lifecycle-request` |

All lifecycle and operational-management selectors require an administrative
principal. Lifecycle responses deliberately omit the idempotency key and
operator identity.
