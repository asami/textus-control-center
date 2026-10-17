# Phase 4 packaged Operations acceptance

CC4-S8A connects native packaged Control Center generated protected REST
Operations to the actual production `textus-supervisor` provider and its real
`cncf <development-directory> server` child. The executable specification is
[`phase4_packaged_operations.py`](../../scripts/test/phase4_packaged_operations.py),
entered through
[`check-phase-4-packaged-operations.sh`](../../scripts/test/check-phase-4-packaged-operations.sh).
It consumes the existing catalog, security, SPI, lifecycle and native packaging
contracts. It introduces no production configuration or source change.

## Explicit prerequisites and invocation

Preparation belongs to the parent through the registered serialized SBT
command group. Build the genuine Control Center and Supervisor CARs using
their native `cozyBuildCar`, with the admitted native dependencies. Their
ABI and component descriptors must identify `org.simplemodeling.textus.ControlCenter`
and `org.simplemodeling.textus.Supervisor` at `0.1.1-SNAPSHOT`. Their native
runtime manifests must include `0.5.3-SNAPSHOT` in their tested CNCF versions.
Export the actual CNCF Launcher's `Runtime / fullClasspath` into a file.
Prepare the CNCF source runtime natively, including its own target `CncfMain`
class and `target/cncf.d/runtime-classpath.txt`. Missing prerequisites fail;
the driver never builds, publishes, resolves a downloaded substitute, or
fabricates native descriptors, ABI manifests, CARs or source overlays.

Before each runtime invocation, the parent also prepares a fresh private
`target/phase-4-packaged-operations/case-fixtures-*` root containing
`embedded/textus-supervisor` and `external/textus-supervisor`. For each fixture,
copy the genuine Supervisor `project.yaml`, `src/main/car`, generated
`target/cozy/model-metadata.json` and its own compiled target directories from
the actual native runtime classpath. Project only classpath entries beneath
the original Supervisor target into the copied fixture target; retain existing
external dependency entries. In the copied
`project.component.config.textus.server.default-port`, select distinct ports
for the two placements, each as exactly one quoted string in `1024..65535`.

The parent invokes the primary Control Center project's native sbt-cozy
`cozyDevelopmentRuntimeManifest` task through the registered serialized SBT
runner, with scoped `baseDirectory`, `target` and `cozyRuntimeClasspathFile`
settings selecting each already-copied fixture and its derived classpath.
That native producer generates the development descriptor, manifest and
integrity evidence; none is handwritten. This preparation modifies neither
the source repositories nor their genuine build outputs. The acceptance driver
runs no producer, Cozy, SBT, build or publication command.

A compatible stable uv-managed Python 3.12 or later must already exist. The
shell entry uses `uv run --no-cache --managed-python --no-python-downloads
--script`. The Python driver independently declares its Python requirement
and empty dependency set through PEP 723. It uses Python's standard library
only, with no installation, bootstrap, system-Python fallback or cache override.

```sh
bash scripts/test/check-phase-4-packaged-operations.sh \
  --java /absolute/existing/jdk/bin/java \
  --cncf-launcher-classpath-file /absolute/native/launcher-classpath.txt \
  --runtime-root /absolute/prepared/goldenport-cncf \
  --control-center-car /absolute/textus-control-center/target/textus-control-center-0.1.1-SNAPSHOT.car \
  --supervisor-car /absolute/textus-supervisor/target/textus-supervisor-0.1.1-SNAPSHOT.car \
  --development-fixtures-root /absolute/textus-control-center/target/phase-4-packaged-operations/case-fixtures-normal \
  --component-version 0.1.1-SNAPSHOT \
  --runtime-version 0.5.3-SNAPSHOT \
  --result-receipt /absolute/textus-control-center/target/phase-4-packaged-operations/case-normal/receipt.json
```

All file inputs must be absolute existing nonempty files. Classpath entries
must be absolute and exist, with the real compiled Launcher main. The runtime
root must contain source and build metadata declaring the exact runtime version.
Each CAR must be a native target output containing matching ABI/descriptor
identity, native runtime manifest and compiled `ComponentFactory` in
`component/main.jar`. These preliminary checks do not replace the production
descriptor, ABI, integrity, runtime and security checks, which remain enabled.

The development fixture root must be absolute and resolve beneath the output
root with a `case-fixtures-*` name. Both placement fixtures must contain
nonempty `project.yaml`, `src/main/car/abi-manifest.json` and
`target/cncf.d/{runtime-classpath.txt,component-descriptor.json,car-runtime-manifest.json}`.
Each classpath must have existing absolute entries and a directory beneath the
fixture's own target containing the genuine Supervisor implementation
`ComponentFactory.class`. ABI and descriptor evidence identify namespace
`org.simplemodeling.textus`, id `Supervisor` and version `0.1.1-SNAPSHOT`.
The native development v2 manifest requires `sourceKind=development-directory`,
`car.name=textus-supervisor`, `car.component=Supervisor`, the selected component
version and tested CNCF runtime. The local id in this development manifest is
the native producer convention; packaged CAR manifests retain their qualified
component identity. Missing, duplicate, unquoted, invalid or privileged
configured fixture ports fail before server acquisition. Inputs remain
read-only, and these preliminary checks leave production admission authoritative.

The receipt selector must be a JSON filename below this repository's
`target/phase-4-packaged-operations/case-*/`. Select a fresh receipt for each
run. A missing or invalid selector fails before output creation. Other argument
or prerequisite failures write a non-success receipt for a valid selector.
Inputs are read-only. All outputs, including retained raw HTTP bodies, JVM
stdout/stderr, private configuration and process records, remain beneath
`target/phase-4-packaged-operations/case-*`.

## Given two private native placements

Given explicit native inputs, the driver creates a private `case-run-*`
directory and separate `case-embedded` and `case-external` directories beneath
it. Each has independent `user-home`, `launcher-home`, `control-center.home`,
`local-data`, `tmp`, runtime catalog, Launcher configuration and unchanged CAR
copies. Reserved numeric IPv4 loopback ports are distinct within each case.
The target reservation binds the exact preconfigured fixture port; Control
Center and external Supervisor reservations use dynamically allocated ports.
Every acquired reservation is retained before further preparation, so cleanup
also closes reservations if any subsequent bind fails.
Each reservation is released immediately before the corresponding native
server or lifecycle child is started. Placements execute sequentially.

The case development catalog selects its preprepared placement fixture and
uses that fixture's parent as its development root. The native Supervisor has
canonical `project.namespace`, `id`, `kind` and component metadata. Its
`project.component.config.textus.server.default-port` is a quoted string.
The catalog is configured through the existing Control Center home and the
same catalog is consumed by the standalone Supervisor profile registry.
Refresh and list Operations supply the actual available `sourceKind` and
`sourceId`; the harness submits those exact fields.

The case `bin/cncf` wrapper invokes the genuine configured production Launcher
JVM, pinned to the native runtime. It preserves all arguments supplied by the
production process host, including the fixture directory, server mode, port,
repository search directory and opaque reserved instance correlation. It adds
the exact native Supervisor CAR and child assembly/runtime inputs. CNCF
Launcher's production server-target normalization retains the development
directory as a repository search source. That source must pass strict native
development evidence admission; metadata alone cannot qualify it. Native
packaged component-file activation still supplies the active compiled child;
the prepared development evidence does not replace packaged activation, and
no source overlay participates.

The embedded case loads both CARs in one Control Center JVM with the existing
SupervisorSocket SPI binding. The external case first acquires a separate
native Supervisor server JVM. The Control Center assembly still loads both
CARs, but selects `textus-supervisor.external.endpoint`,
`textus-supervisor.external.authentication.token` and
`textus-supervisor.external.timeout`. The existing provider forwards to the
real remote generated Supervisor `submit` and `lookup` Operations.
The remote service uses its own distinct case credential and configured
`textus-supervisor-service` authentication provider with `enabled: true` and
`default: false`, disabled fallback privilege and no local operator subject.
Only this separately placed service uses `textus.subsystem.user-mode: multi-user`
to select the authenticated-user execution profile. Control Center and the
native child retain `standalone` mode with their local operator subject.
The remote generated Operations remain protected by the configured service
authentication. It is never a fixture HTTP backend.

Control Center uses the existing standalone local operator subject, so local
calls with no Bearer may administer it. Launcher registration has a separate
case machine credential and its unchanged registration-only privilege.
The Launcher `registration.endpoint` is the full generated SubsystemInventory
operation base,
`http://127.0.0.1:<control-center-port>/rest/v1/org.simplemodeling.textus.ControlCenter/SubsystemInventory`.
Production Launcher appends its existing `register-subsystem`,
`heartbeat-subsystem` and `deregister-subsystem` operation names to that base.
Internal generated REST clients use the Control Center origin URL separately.
Authorization denial deliberately sends an explicit invalid Bearer, which
suppresses local-subject fallback. A structured HTTP 401/403 must leave audit
and the target port unchanged; missing-token behavior is not a denial oracle.

Inherited development selector, delegation and argument-file environment
variables are cleared using the accepted cross-Launcher driver's list.
Case `PATH` selects only the case wrapper before the inherited search path.
The script preserves `HOME`, `CODEX_HOME` and normal cache settings; explicit
JVM `user.home`, temporary directory and Launcher home select case state.
No global configuration, warehouse or source repository is modified.

## When generated protected Operations exercise the native child

The driver calls generated canonical routes under
`/rest/v1/org.simplemodeling.textus.ControlCenter/` using the declared
`CarCatalog`, `OperationalManagement` and `LifecycleControl` service and
operation names. Every call explicitly selects `textus.format=json`,
`textus.output.shape=data` and `textus.command.execution-mode=sync`.
The expected data shape is the record itself, with CNCF's default snake_case
output field names; paginated records contain their `data` array and a
nonnegative integer `total_count`, except that native `Record.dataAuto` omits
the empty `data` array for the exact zero-total page with integer `offset=0`
and `limit=100`, which the driver interprets as an empty list. Malformed,
truncated, error, envelope and incomplete page
responses fail rather than being treated as empty success.

For each placement, three rounds generate distinct random idempotency keys:

1. Start request detail must reach exactly `accepted`; only `queued` may be
   polled while waiting. It must retain nonempty request, Supervisor and
   instance IDs. A separate protected catalog observation must reach `running`
   for exactly that instance, and the actual target port must be ready. Its
   exact retry also returns `accepted`, retains the same correlation, repeats
   the correlated runtime and port checks, and adds only one audit record
   across both calls.
2. Restart must reach `accepted` with a new opaque instance. Both the original
   call and its exact retry require the separate correlated `running` catalog
   observation and target port readiness. The retry preserves that
   instance/request and adds only one audit record.
3. Stop must reach `stopped`, identify the restarted instance and release the
   target port. Its exact retry preserves correlation with only one audit.

The standalone Supervisor durably retains its `accepted` ownership result
after owned-child readiness; lookup does not turn that result into `running`.
The former harness oracle conflated request state and runtime projection.
This acceptance corrects that oracle while requiring both ownership and
runtime evidence for every Start/Restart, including every exact retry.

The bounded runtime observation calls generated protected
`CarCatalog/getManagedCar` with `artifactId=textus-supervisor`. Its data record
must identify that exact `artifact_id`, have a string `runtime_state` of
exactly `running`, and supply `active_instance_ids` as a list equal to exactly
`[instance_id]` from the accepted lifecycle result. The reserved ID must be
nonempty. Only `not-ready`, `not-running` and `starting` may be polled until
`running`; other states fail. A present active-ID field must always be a list
of nonempty strings. Native `Record.dataAuto` may omit the empty list only
while observing `not-running` or `starting`; a `running` projection requires
the explicit exact singleton list. Missing identity/state, null or malformed
fields, and different or duplicate running instance IDs fail. The observation
and each HTTP exchange share the lesser of the 35-second observation deadline
and the case deadline. Timeout fails. The driver reads neither SQLite nor the
raw Launcher store, and port readiness alone cannot establish success.

Request detail and list audit recursively exclude `operatorSubjectId` and
`idempotencyKey` in their output spellings. `observedCases` retains both actual
accepted lifecycle results and the original/retry runtime projections. Stop
continues to require exactly `stopped` and a reusable target port, including
its exact retry.

During the first running round, Remove must set `excluded` without stopping
the native target. Refresh must preserve exclusion, and the normal management
list must omit the target. The existing implementation also rejects an explicit
Stop while excluded, with `component-not-managed`; this acceptance retains
that policy. Restore must return the original available DEV target to
`auto-managed` before later lifecycle actions. Remove never becomes an implicit
Stop. Any contradiction fails and must be returned to the parent for diagnosis.

## Then owned cleanup and receipts describe the actual outcome

Server startup is bounded to 90 seconds per JVM. Each placement has a
420-second client budget, request URL limit of 8192 bytes, response limit of
1 MiB, and a 25-second total HTTP deadline including headers and body.
Lifecycle detail polling has a 35-second deadline within the case budget.
HTTP connects directly to numeric loopback, follows no redirect, and preserves
raw responses. Given/When/Then messages appear at setup, action and expectation
boundaries. Lifecycle and audit assertions use the generated Operations; no
raw Launcher evidence store, evidence-derived PID or process scan is used.

Every successful `Popen` acquisition is immediately retained with exact argv,
cwd, positive PID, observed start time and separate persistent stdout/stderr.
On POSIX macOS, acquisition blocks SIGINT and SIGTERM with
`signal.pthread_sigmask` before `Popen` and restores the previous mask in
`finally`, after retaining the returned handle and ownership record. A failed
acquisition records its actual error before restoring the mask. Pending signals
cannot interrupt the interval between successful acquisition and retention.
This stdlib harness is single-threaded. A scoped POSIX `preexec_fn` restores
the captured pre-acquisition mask in the child before exec, so the native JVM
retains normal INT/TERM handling while the parent completes handle retention.
Servers use `start_new_session=True`; their acquired process-group identity is
the acquired child's PID established by `setsid`. Supervisor-owned native
children inherit that group. Process records describe harness ownership and
never derive ownership from product evidence.

Cleanup runs after preparation, startup or client failures and after success.
Common SIGINT/SIGTERM handlers are installed before child acquisition. They
record the first received signal and preserve any earlier primary error;
explicit interruption checks outside cleanup turn the recorded signal into a
run failure. The handlers defer repeated signals throughout mandatory cleanup,
including protected Stop, every group and port attempt, and persisted receipts.
They never throw out of cleanup. A signal first received during cleanup still
sets non-success, independently of successful process termination. Previous
handlers are restored only after mandatory cleanup and final receipt attempts.
The separate SIGALRM HTTP deadline remains active during protected Stop.
For a possibly started lifecycle target it first attempts a bounded generated
protected Stop, restoring management first if the case was interrupted while
excluded. The possibly-started obligation is set before Start submission and
cleared only after the complete normal idempotent Stop succeeds: both calls
report `stopped`, both port-release checks succeed, request/instance/Supervisor
correlation and the one-record audit check pass, and Stop identifies the
restarted instance. Any earlier exception or the deliberate live-Start failure
retains the obligation. A fully confirmed normal Stop therefore skips a new
different-key cleanup Stop; acquired-group and port cleanup still run.
Every acquired group is then attempted independently: TERM and
bounded wait, followed when required by KILL and bounded wait. Only these
acquired case groups, including their inherited descendants, receive signals.
Direct `Popen` children are reaped, acquired group absence is checked, and every
case port must be reusable within ten seconds. Both normal Stop and mandatory
cleanup require an absent TCP listener followed by a successful real numeric
loopback bind with `SO_REUSEADDR` only. `SO_REUSEPORT` is never enabled. Listener
presence or bind failure continues bounded polling, with the last failure
retained in the timeout diagnostic. This permits native HTTP address reuse
after passive TIME_WAIT without accepting a live listener or listener absence
alone. TERM waits eight seconds and
KILL waits five seconds per group. No `ps`, `pgrep`, global `pkill`, arbitrary
PID lookup or evidence-derived signalling is permitted. Descendants are covered
by group shutdown; the script does not claim they were individually waited.

Initial and final receipts are atomic. They contain `readiness`, `clients`,
`cleanup`, `processCompletion`, `success`, `error`, `cleanupDiagnostics` and
`runDirectory`, plus per-placement observations. `processCompletion` becomes
`terminal` only after cleanup completes with reaped direct children, absent
owned groups and reusable ports. Cleanup diagnostics remain separate from the
primary failure. A failure receipt and nonzero exit never mean PASS. Normal
PASS requires both placements, all client assertions and complete cleanup.

After the acquired native Control Center server reaches startup readiness,
each placement atomically writes `server-process.json` in its case directory.
This object copies the retained Control Center process record, including its
positive `pid`, observed `startedAt` and exact `argv`. Its `baseUrl` is the
actual numeric loopback Control Center URL with explicit port.
`supervisorBaseUrl` is the same URL in embedded placement and the actual
separately acquired Supervisor server URL in external placement. The complete
`processes.json` array remains available. These records describe acquired
production JVMs; they do not introduce another process or endpoint.

The top-level receipt adds `processDirectory` only after the first such native
process object exists and retains that first case directory. The existing
typed session consumer selects `directory_selector=["processDirectory"]`,
`relative_path="server-process.json"`, `pid_field="pid"`,
`started_field="startedAt"` and
`endpoint_fields=["baseUrl", "supervisorBaseUrl"]`. Preparation and
no-acquisition failures cannot fabricate this selector or claim readiness.

For a separate deliberately failing live run, repeat the same native-input
command with `--inject-failure-after-start`, a fresh parent-prepared
`--development-fixtures-root` and a fresh receipt such as
`case-injected/receipt.json`. Only the embedded case runs: after a successful
Start and exact retry, each with accepted ownership, exact correlated running
projection and target port readiness, it raises the explicit injected primary failure. It
must exit nonzero, retain that primary error and perform the same protected
Stop and complete acquired-group cleanup. A successful cleanup does not turn
this deliberate failure into acceptance success.

This is executable behavior acceptance with generated variation, not a
property proof for every possible key, source, failure or interleaving.
It establishes a connected packaged Operations route through embedded and
external production Supervisor placement. Actual panel handlers/browser
behavior and reconstruction after independent JVM replacement remain separate
acceptance work. Default Java 17 execution and final full Phase validation
remain independent obligations. Existing Phase 1–3 catalog/registration semantics are consumed
unchanged. No new example-based unit tests or execution-evidence package are
introduced, and retained target outputs are never committed.
