# Textus Control Center

Textus Control Center is a Cozy-generated CAR that inventories Subsystem server
invocations started through the canonical Textus and CNCF launchers. Phase 1
does not control processes; it records launcher registration, heartbeat, and
normal termination facts and projects the same inventory through CNCF command,
REST, and Web surfaces.

Component:
- CAR artifact: `textus-control-center`
- component: `textus-control-center`
- package: `org.simplemodeling.textus.controlcenter`
- version: `0.1.0`

Typical workflow:
- `sbt cozyGenerate`
- `sbt compile`
- `sbt cozyBuildCar`

Standalone bootstrap creates a machine-local Control Center installation below
`~/.cncf/textus-control-center` without placing its launcher credential in this
repository or launcher configuration:

```sh
bash scripts/bootstrap-standalone.sh
cncf --cncf-config ~/.cncf/textus-control-center/server-config.yaml \
  /absolute/path/to/textus-control-center server
```

Use `--cncf-home <temporary-path>` when testing the bootstrap itself. Re-running
the script preserves the installation identity; `--rotate` replaces only the
local launcher credential. The script never prints that credential.

Configure the local CAR catalog separately. It records explicit development
roots, the CNCF local CAR catalog, and only the public artifacts that the
operator subscribes to; it never scans a public directory listing:

```sh
bash scripts/configure-standalone-catalog.sh \
  --development-root /absolute/path/to/src/dev2026 \
  --public-subscription textus-user-notification
```

This writes `~/.cncf/textus-control-center/catalog.yaml`. Restart Control
Center after changing it. Add `--public-subscription` once per additional
SimpleModeling.org CAR to manage.

The generated `SubsystemInventory` service owns these Phase 1 operations:

- `registerSubsystem`
- `heartbeatSubsystem`
- `deregisterSubsystem`
- `listSubsystems`
- `getSubsystem`

The launcher integration applies only to `textus <artifact> server`,
`cncf server`, and `cncf <target> server`. Deprecated `cncf dev server` is not
part of this component's contract.

Generated Scala sources are written under
`target/scala-3.3.7/src_managed/main`.
