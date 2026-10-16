# Phase 4 cross-Launcher acceptance

This CC4-S7A acceptance specifies the shared Launcher evidence boundary before
Control Center is available. Its executable orchestration is
`scripts/test/check-phase-4-cross-launcher-acceptance.sh`, backed by
`scripts/test/phase4_cross_launcher_acceptance.py`. It uses real native server
JVMs and the production evidence-list CLI. It does not change Scala semantics.

## Preparation and inputs

Preparation is a separate parent-owned activity through the registered SBT
command group and its serialization wrapper. The driver never prepares,
compiles, publishes, or delegates to SBT or Cozy. Export both Launchers' actual
`Runtime / fullClasspath` outputs into files. Prepare the CNCF runtime and
Supervisor development runtime natively, including the component's own target
classes, descriptor, development runtime manifest, classpath and source ABI
manifest. Build the genuine Supervisor CAR through native `cozyBuildCar`.
Preparation must preserve the declared packaging policy; admission or ABI
failures require parent diagnosis, never fabricated metadata or a fake server.

Run the shell entry with these explicit arguments:

| Argument | Contract |
| --- | --- |
| `--java` | Absolute existing executable Java path. |
| `--cncf-launcher-classpath-file` | Absolute file of existing absolute native classpath entries containing `cncf.launcher.CncfLauncherMain`. |
| `--textus-launcher-classpath-file` | Absolute file of existing absolute native classpath entries containing `textus.launcher.TextusMain`. |
| `--runtime-root` | Absolute prepared CNCF checkout, with native `target/cncf.d/runtime-classpath.txt` and its own target `CncfMain` class. |
| `--component-root` | Absolute prepared Supervisor checkout, including its own target `ComponentFactory` class in its native runtime classpath. |
| `--car-file` | Absolute genuine built CAR beneath that component's `target/`; read and copied unchanged. |
| `--artifact-id` | Explicit CAR artifact ID, matching native CAR metadata. |
| `--artifact-version` | Explicit Supervisor component/CAR version, matching native ABI and CAR metadata. |
| `--runtime-version` | Explicit concrete prepared CNCF version, matching the runtime build; dynamic selectors are rejected. |
| `--result-receipt` | Absolute JSON output path strictly beneath this project's `target/phase-4-cross-launcher/`. |

Artifact IDs and versions are coordinates, not filesystem paths. The driver
requires all inputs and fails on missing prerequisites. It creates a unique
`target/phase-4-cross-launcher/case-*` directory. Input paths grant read access
only; no source, installed warehouse or global configuration is modified.
The shell uses managed Python through
`uv run --no-cache --managed-python --no-python-downloads --script`; a compatible
uv-managed stable CPython 3.12 or later must already be available. No system
Python fallback is used.

## Given native preparation and an unavailable notification endpoint

Given both real Launchers, a prepared native runtime, and the actual Supervisor
development component and built CAR, the driver validates prerequisites before
starting children. It copies exactly that CAR into a case-private repository
at `<artifact-id>/<version>/<artifact-id>-<version>.car`.

The case assembly selects `org.simplemodeling.textus.Supervisor` at the supplied
component version, standalone user mode, a local operator, and case-local data.
Its structure follows `CanonicalSupervisorProcessFixture`. A declared fixture
catalog under case home `.cncf/catalog/textus/runtime-catalog.yaml` contains only
the supplied actual development runtime coordinate. The runtime selection is
pinned; no catalog fetch or selector derivation is needed.

Both Launchers use the same explicit case `--launcher-home`. Their JVM argv
sets `-Duser.home` and `-Djava.io.tmpdir` to case paths. `HOME` and `CODEX_HOME`
are preserved; normal cache environment settings are preserved. Launcher
development/delegation and argument-file selectors are removed from the child
environment. Native CNCF main may apply its own application-home policy; case
Launcher state is selected explicitly and runtime data/configuration is passed
explicitly. No installed Control Center locator is used for notification:
registration is explicitly enabled with endpoint `http://127.0.0.1:1`, a
case-only token environment variable, bounded timeout, and a host label.

## When two independent canonical servers start

When the CNCF JVM executes `cncf server` from the Supervisor checkout with
`--runtime-dev-dir` and explicit native runtime configuration, and a distinct
Textus JVM executes `<artifact-id>.car:<version> server` with the private CAR
repository and that runtime, both must start despite unavailable notification.
No Launcher boot script, fake host or network server participates.

Two bound numeric IPv4 loopback sockets reserve distinct positive ephemeral
ports. Each reservation is released immediately before its server starts.
Actual TCP readiness is polled with a startup deadline while both acquired
server handles remain live. All process acquisition facts, argv, cwd, log paths
and PIDs are retained separately from product evidence. They prove independent
native JVM invocation; product evidence never grants process ownership.

## Then the safe shared evidence survives both writers and normal stops

Then a separate bounded CNCF Launcher JVM runs
`launcher evidence list --format json` against the same case Launcher home.
Only its safe versioned projection is used for acceptance, never the raw store.
Every projection has exactly two distinct instance IDs with `cncf` and `textus`
kinds, `development` and `artifact` modes respectively, and the correct
Supervisor/CAR targets. Its rows contain only the documented list fields, with
no development directory, process identity, credentials, environment or
unrestricted command arguments. The case-only credential cannot appear in it.

Three successive observations, separated by polling intervals, must preserve
the same two current IDs while both actual JVM handles remain live. Next the
driver sends TERM through the acquired Textus handle and waits for normal
shutdown without KILL. The same projection must show Textus `stoppedAt` while
CNCF remains current and live. Finally CNCF receives TERM through its handle;
the same two IDs must both have normal `stoppedAt` values. A forced termination
cannot satisfy these normal-stop assertions.

## Cleanup and results

Every subprocess, including each evidence reader, has persistent stdout/stderr
files and a bounded wait. Startup is bounded to 90 seconds per server, evidence
observation to 30 seconds per stage, each reader to 15 seconds, normal TERM to
15 seconds, cleanup TERM to 5 seconds and cleanup KILL to 5 seconds per child.
Port reuse is checked within 10 seconds. Errors identify the retained case and
relevant logs. Missing inputs or CAR/ABI errors fail; they are never skips.

A `finally` boundary attempts cleanup for every acquired `Popen` handle even
after a primary error. It attempts bounded TERM and, when necessary, bounded
KILL using only those handles, then reaps them. There is no process scan,
descendant discovery, arbitrary PID, port signalling or user-process cleanup.
It checks that both selected ports can be rebound. Cleanup diagnostics are
secondary to the original failure. Success and failure cases are retained.

The selected receipt and case JSON files are atomically written within the
output root. The receipt records `readiness` only after both native servers are
TCP-ready, `clients` only after every safe-projection and normal-stop assertion,
and `cleanup` only after all acquired children are reaped and both ports are
reusable. `processCompletion` becomes `terminal` only after complete cleanup;
otherwise it remains `incomplete`. Failure never promotes an incomplete flag
to success. `runDirectory` identifies the actual unique case.

Case `server-process.json` contains the acquired CNCF positive integer `pid`,
its observed acquisition `startedAt`, and explicit numeric loopback `baseUrl`
and `textusBaseUrl`. `processes.json` separately records both JVM acquisitions
and every reader acquisition; these ownership facts do not replace acceptance
assertions. A receipt selector outside the admitted output root is rejected
without writing outside it. Argument failures with a valid receipt selector
produce a failure receipt; missing or invalid receipt selectors fail before
output creation.

This slice establishes shared evidence and notification availability isolation.
Control Center reconciliation, panel actions, protected lifecycle Operations,
embedded/external placement comparison and JVM restart remain separate
acceptance scopes. Runtime files and receipts are local review evidence and
must not be staged or committed.
