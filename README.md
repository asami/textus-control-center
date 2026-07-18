# Textus Admin

Textus Admin is a Cozy-generated CAR that inventories Subsystem server
invocations started through the canonical Textus and CNCF launchers. Phase 1
does not control processes; it records launcher registration, heartbeat, and
normal termination facts and projects the same inventory through CNCF command,
REST, and Web surfaces.

Component:
- artifact: `textus-admin`
- package: `org.simplemodeling.textus.admin`
- version: `0.1.0-SNAPSHOT`

Typical workflow:
- `sbt cozyGenerate`
- `sbt compile`
- `sbt cozyBuildCAR`

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
