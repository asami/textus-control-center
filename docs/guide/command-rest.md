# Subsystem Inventory Command and REST Reference

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
