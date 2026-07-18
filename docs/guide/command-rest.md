# Subsystem Inventory Command and REST Reference

The canonical Command selectors are generated from the Cozy component and
service names. They are intentionally distinct from deprecated `cncf dev
server` behavior.

| Operation | Command selector | Automatic REST path |
|---|---|---|
| Register | `textus-admin.subsystem-inventory.register-subsystem` | `GET /rest/v1/textus-admin/subsystem-inventory/register-subsystem` |
| Heartbeat | `textus-admin.subsystem-inventory.heartbeat-subsystem` | `GET /rest/v1/textus-admin/subsystem-inventory/heartbeat-subsystem` |
| Deregister | `textus-admin.subsystem-inventory.deregister-subsystem` | `GET /rest/v1/textus-admin/subsystem-inventory/deregister-subsystem` |
| List | `textus-admin.subsystem-inventory.list-subsystems` | `GET /rest/v1/textus-admin/subsystem-inventory/list-subsystems` |
| Detail | `textus-admin.subsystem-inventory.get-subsystem` | `GET /rest/v1/textus-admin/subsystem-inventory/get-subsystem` |

For example, an authenticated administrative command invocation is:

```sh
cncf command textus-admin.subsystem-inventory.list-subsystems
```

The corresponding REST request is:

```sh
curl --fail-with-body \\
  -H "Authorization: Bearer $TEXTUS_ADMIN_TOKEN" \\
  'https://admin.example.test/rest/v1/textus-admin/subsystem-inventory/list-subsystems?text=sample&offset=0&limit=100'
```

Registration operations require an authenticated launcher principal. List and
detail require an administrative principal. Missing, invalid, conflicting, and
unauthorized requests are returned as CNCF structured Conclusions; automatic
REST maps them to the corresponding HTTP status. The projected response never
contains `registrationPrincipalId`.

`TextusAdmin.meta.openapi` is the machine-readable source of the automatic
REST paths. `scripts/check-admin-read-flows.sh` verifies the generated
selectors and OpenAPI routes.
