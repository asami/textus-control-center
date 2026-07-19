# Standalone Locator and Credential Bootstrap

## Purpose

This design defines the one-machine registration setup used by standalone
Textus Control Center. It makes the CNCF launcher home the shared discovery
point so both `cncf` and `textus` launchers obtain the same local registration
context without adding endpoint or credential arguments to each CAR launch.

The normative fields and precedence rules are in
`docs/spec/standalone-locator-bootstrap.md`.

## Local Layout

The standalone installation owns the following untracked machine-local tree:

```text
~/.cncf/textus-control-center/
  standalone-locator.yaml
  credentials/
    launcher-registration.token
  state/
```

`standalone-locator.yaml` is the shared locator. It contains only public local
connection facts, scope/install identity, and a relative credential reference.
The token value is stored only in the referenced credential file. The state
directory is reserved for the standalone assembly's durable installation and
registry state; launchers do not read or write it.

The Textus launcher resolves this layout through its existing `cncfHome`, even
though its ordinary user configuration remains under `~/.textus`. This makes
the locator a CNCF-machine integration contract rather than a duplicate Textus
configuration system.

## Bootstrap and Recovery

`scripts/bootstrap-standalone.sh` creates the directory with owner-only
permissions, creates a stable installation identity and scope ID, creates a
fresh launcher token, writes the locator atomically, and configures the local
Control Center assembly to accept that token. Bootstrap never prints the token.

Rotation creates a new token and atomically updates both the local assembly
credential and the credential file. Loss or permission failure disables
automatic registration with a sanitized warning; it never blocks a CAR server
from starting. Recovery is an explicit bootstrap/rotate operation, not a
launcher attempt to recreate credentials. The script accepts `--cncf-home` so
executable checks can use a target-owned directory instead of the user's real
launcher home.

## Resolution Order

For a canonical server launch, a launcher resolves registration in this order:

1. An explicit `enabled: false` disables registration completely.
2. An explicit `profile: control-plane` registration configuration is used only
   when it supplies an endpoint and a credential reference; it never falls back
   to standalone data.
3. An explicit standalone registration configuration may override selected
   safe locator values, but obtains the credential only through a reference.
4. A valid machine-level standalone locator supplies the default local
   registration context.
5. Without a valid explicit configuration or locator, registration is disabled
   and the target server starts normally.

The existing Phase 1 `token-env` form remains a compatibility input during the
migration. It is lower priority than an explicit credential reference and is
not written by standalone bootstrap.

## Base URL Policy

The launcher's explicit external base URL wins. For standalone registration,
an explicit `--textus.server.port` or `--cncf.server.port` supplies
`http://127.0.0.1:<effective-port>`. Otherwise the launcher waits for the same
effective server configuration/port assignment supplied to CNCF runtime. It
must not guess a host address or publish a default port when runtime selected
another instance port.

## Security Boundary

Credential references are relative paths under the standalone root. Resolution
must reject absolute paths, parent traversal, symlink escapes, missing files,
and insecure file permissions. Neither locator parsing nor warnings may expose
the token, credential path, complete endpoint query, local state path, or raw
server configuration.

Control-plane mode is intentionally explicit: it does not read standalone
credentials and cannot inherit a loopback base URL silently.
