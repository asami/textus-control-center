status = draft
scope = standalone locator, credential bootstrap, and registration precedence

# Standalone Locator Bootstrap Specification

## 1. Locator

The canonical machine-level standalone locator is:

```text
~/.cncf/textus-control-center/standalone-locator.yaml
```

Its schema version is `1` and it contains these logical fields:

| Field | Required | Rule |
| --- | --- | --- |
| `profile` | yes | Must be `standalone`. |
| `scopeId` | yes | Stable local management scope identity. |
| `installationId` | yes | Stable local installation identity. |
| `endpoint` | yes | Absolute loopback Control Center registration base URL. |
| `credentialRef` | yes | Relative reference beneath the locator root. |
| `timeout` | no | Positive duration; defaults to `5s`. |
| `heartbeatInterval` | no | Positive duration; defaults to `30s`. |
| `hostLabel` | no | Safe local label; no environment dump. |

The locator does not contain a token value, external endpoint configuration,
or durable registry data.

The `5s` default bounds each registration or heartbeat request with headroom
beyond normal local operation response latency (normally about 2.1–2.3 seconds)
while remaining below the `30s` heartbeat interval.

## 2. Credential Reference

`credentialRef` must resolve below
`~/.cncf/textus-control-center/credentials/`. The referenced token file is
owner-readable only and contains one non-empty token value. A launcher must
reject an absolute path, traversal outside the root, symlink escape, missing
file, empty file, or insecure permissions as an unavailable credential.

The token value and the reference path must not appear in command output,
warnings, logs, registry records, REST responses, or Web projections.

## 3. Bootstrap Lifecycle

`scripts/bootstrap-standalone.sh` creates a stable `scopeId` and
`installationId`, a fresh token file,
and a valid locator atomically. It configures the standalone Control Center to
authenticate the corresponding launcher principal. Rotation replaces the token
atomically and invalidates the previous token. Credential loss requires an
explicit bootstrap or rotation recovery; launchers must not recreate it.

## 4. Launcher Resolution

Both `cncf` and `textus` launchers resolve the same locator path through the
machine CNCF home. For canonical server execution, the result is selected by
the following precedence:

1. Explicit `textus-control-center.registration.enabled: false` returns no
   registration session.
2. Explicit `profile: control-plane` requires explicit endpoint and credential
   reference; an incomplete configuration is invalid and does not use the
   standalone locator.
3. Explicit standalone values override the corresponding locator safe fields;
   credentials remain references.
4. A valid standalone locator supplies registration context.
5. No valid context returns no registration session.

The legacy `token-env` input remains readable solely for Phase 1 migration. It
is not produced by standalone bootstrap and loses to an explicit credential
reference.

## 5. Base URL

An explicit external `base-url` is preserved. Otherwise an explicit
`--textus.server.port` or `--cncf.server.port` supplies the effective local
port and standalone registration uses `http://127.0.0.1:<effective-port>`.
Without that explicit port, registration waits for the same effective port
assigned to the launched server by CNCF runtime. If no safe effective port is
available, the launcher must skip registration and continue server startup.

Control-plane mode must have an explicit external base URL or an independently
documented external URL resolver; it must not inherit standalone loopback
assumptions.

## 6. Failure Isolation

Invalid locator data, unavailable credentials, bootstrap mismatch, resolution
errors, timeout, non-success response, heartbeat failure, and deregistration
failure produce only sanitized diagnostics. Each condition leaves the managed
CAR server lifecycle unchanged.
