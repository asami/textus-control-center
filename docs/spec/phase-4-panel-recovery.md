# Phase 4 packaged panel and JVM recovery acceptance

CC4-S9A connects the actual packaged operating panel to protected Operations,
the production Supervisor and a native Launcher child, then reconstructs the
independent service JVMs using the same private durable state. The executable
specification consists of
[`phase4_panel_recovery.py`](../../scripts/test/phase4_panel_recovery.py) and
[`phase4_panel_client.cjs`](../../scripts/test/phase4_panel_client.cjs), entered
through
[`check-phase-4-panel-recovery.sh`](../../scripts/test/check-phase-4-panel-recovery.sh).
It runs embedded placement followed by external placement exactly once.

## Native prerequisites and invocation

The native Java, Launcher classpath, CNCF runtime, genuine packaged CARs and
prepared development fixtures have the same admission requirements as
[packaged Operations acceptance](phase-4-packaged-operations.md). The component
version is `0.1.1-SNAPSHOT`; the tested runtime version is `0.5.3-SNAPSHOT`.
The existing `phase4_packaged_operations.py` driver is imported with bytecode
writing disabled and remains unchanged. Its native admission, preparation,
assemblies, configuration, launch, protected Operations, safe audit, request
detail, correlated runtime observation and cleanup are reused.

The private child process environment omits only inherited `HOME`, preserving
the inherited sanitization and every other environment entry. The parent
environment, including `HOME` and `CODEX_HOME`, remains unchanged; no replacement
`HOME` is assigned. Each inherited Java command retains its explicit private
`-Duser.home` and aligns the existing `--launcher-home` value with that same
case-local home. This applies to Control Center, external Supervisor and target
wrapper JVMs, so the native Launcher writer and Supervisor reader use the same
private evidence root. The inherited separate launcher-home directory may remain
unused. Native cache, read/write roots, launch timing and cleanup are preserved.

An accepted native fixture may be reused read-only when its relevant inputs
have not changed. Reuse is recorded as reuse, without fresh native producer
execution credit. All native preparation belongs to the parent workflow.
This route runs no SBT, Cozy, producer, package manager, installation or
download. Native CAR, descriptor, integrity, SPI, authentication, catalog and
registration admission stay enabled.

The existing uv-managed Python 3.12 or later is selected with `--no-cache`,
`--managed-python`, `--no-python-downloads` and `--script`. PEP 723 declares no
Python dependencies. Three additional arguments explicitly select the existing
bundled Node executable, Playwright package and installed Google Chrome:

```sh
bash scripts/test/check-phase-4-panel-recovery.sh \
  --java /absolute/existing/jdk/bin/java \
  --cncf-launcher-classpath-file /absolute/native/launcher-classpath.txt \
  --runtime-root /absolute/prepared/goldenport-cncf \
  --control-center-car /absolute/textus-control-center/target/textus-control-center-0.1.1-SNAPSHOT.car \
  --supervisor-car /absolute/textus-supervisor/target/textus-supervisor-0.1.1-SNAPSHOT.car \
  --development-fixtures-root /absolute/textus-control-center/target/phase-4-packaged-operations/case-fixtures-native \
  --component-version 0.1.1-SNAPSHOT \
  --runtime-version 0.5.3-SNAPSHOT \
  --node /Users/asami/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node \
  --playwright-package-root /Users/asami/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright \
  --browser-executable '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' \
  --result-receipt /absolute/textus-control-center/target/phase-4-packaged-operations/case-panel/receipt.json
```

All old required native arguments retain their names and meaning. Executables
must resolve to the selected regular executable files; the explicit package
must contain actual Playwright metadata and its entry point. Receipt selection
uses the inherited `receipt_selector`. Missing prerequisites fail without
fallback. The earlier deliberate failure switch is not accepted or replayed.

## Browser ownership and panel behavior

Given each private native placement, Python appends one reserved numeric
loopback debugging port after the inherited Control Center, target and optional
external Supervisor ports. It releases that reservation immediately before
acquiring Chrome with the inherited `Case.acquire` ownership protocol.
Chrome has its own process group, private case-local user-data directory,
`--remote-debugging-address=127.0.0.1`, the exact reserved debugging port,
`--headless=new`, `--no-first-run`, `--no-default-browser-check`,
`--disable-background-networking`, `--disable-component-update`,
`--disable-sync` and `--disable-extensions`. The sandbox remains enabled.
Chrome must remain live and become ready within 30 seconds.

When Python acquires Node, it supplies the absolute client script, exact CDP
origin, actual Control Center origin, target artifact, result path and package
root. Playwright connects over CDP only to that newly acquired numeric
loopback origin. It creates a private context with a 1280 by 900 viewport and
blocks page requests outside literal `127.0.0.1`. Python launches Chrome;
Playwright never launches another browser or attaches to an existing user
profile. Node performs no subprocess, server, Git or source-file operations.

Then the browser exercises the actual packaged `/web/textus-control-center`
page with native handlers and protected automatic REST:

1. Given exactly one `tr[data-artifact-id="textus-supervisor"]`, require
   stopped or not-running state, Start enabled, Stop and Restart disabled, and
   no visible operational error.
2. When actual Start is clicked, observe disabled busy lifecycle controls.
   Then require running state, `Start succeeded` feedback, Start disabled and
   Stop and Restart enabled. Save the running screenshot.
3. Given that running row, click the row itself. Then inspect the actual
   dialog's artifact, runtime, management and DEV source fields and close it
   through its Close button.
4. When actual Restart is clicked, observe busy controls and then running state,
   `Restart succeeded` feedback and restored controls. When actual Stop is
   clicked, require its busy state, stopped or not-running state,
   `Stop succeeded` feedback and the initial enabled-control arrangement.
5. Given the stopped managed target, click actual Remove and require its row
   absent. When actual Refresh completes, require exclusion retained and save
   the excluded screenshot. Restore is subsequently a protected Operation;
   there is no claim that the panel has a Restore button.

Each setup, action and expectation is narrated with Given, When and Then
adjacent to its boundary. No direct REST action substitutes for a UI action;
there is no mock fetch, backend substitution, source injection, synthetic DOM
event or alternate successful selector. The exact whole visible lifecycle
feedback `Start succeeded; runtime is running. Open App URL is not yet available.`
or `Restart succeeded; runtime is running. Open App URL is not yet available.`
is retained and deduplicated in the Node receipt's
`optionalApplicationDiagnostics`, despite its current error CSS tone. The Open
App URL is optional; this diagnostic still requires authoritative running
state, action success feedback, restored controls and the independent safe
audit and target-port verification below. Fatal page errors, visible operational
errors and every other error-toned lifecycle message, including URL
synchronization failure, fail with retained diagnostics. Screenshots come from
the real page.

Node writes an atomic initial receipt before CDP connection and a terminal
receipt in `finally`, retaining actions, errors and screenshot paths. It closes
its private context and disconnects; Python retains Chrome's acquired group.
Python bounds the Node wait at 240 seconds and accepts only exit zero plus an
explicit successful receipt. Timeout or forced termination never means PASS.

After the browser, Python requires exactly three new safe protected lifecycle
audit records for Start, Restart and Stop, with distinct request IDs and one
stable nonempty opaque Supervisor identity. Start and Restart have different
nonempty instance IDs; Stop identifies the restarted instance. Existing audit
records remain intact. The target port must have no listener and accept a
real `SO_REUSEADDR`-only bind. The possibly-started flag is set before browser
actions and cleared only after this corroboration. Exclusion is conservatively
marked before browser Remove; protected Restore must confirm `auto-managed`
before reconstruction begins.

## Same-state independent JVM reconstruction

Given a newly generated Start key, Python marks a possible live target before
submitting actual protected Start. Then it retains exact request, Supervisor
and instance IDs, the safe original audit, and the actual correlated registered
running projection plus target TCP readiness.

Before replacing any JVM, read the genuine private
`user-home/.cncf/launcher/server-evidence.json`. Then require schema
`cncf.launcher.server-evidence.v1` and exactly one entry matching the retained
Start instance ID, with a positive integer `processId`, nonempty
`processStartedAt`, `artifactId` equal to `textus-supervisor` and `executionMode`
equal to `development`. Retain only the evidence path and opaque instance
correlation in `observedCases`, without copying the evidence body or credentials.
This is the existing native recovery ownership input prerequisite; missing or
invalid evidence fails. No evidence is written, copied, moved or injected.

When replacing a service, it calls `terminate()` on only the exact retained
direct parent `Popen`, waits at most eight seconds and reaps that parent.
A parent that does not exit fails the scenario. No SIGKILL simulates recovery;
mandatory final cleanup still owns escalation. The old group is retained and
is not stopped during reconstruction, because its target may survive there.
No process discovery, evidence-PID signalling, PID adoption, persistent-file
deletion, handwritten product evidence or ownership tampering occurs.

Then the service port must become reusable and inherited native launch starts
a generation-specific JVM using the same port, configuration, assemblies, CARs,
private homes and durable state. Old and new direct process identities and the
old parent's actual reaping are recorded. The new PID must differ.

In embedded placement the combined Control Center/Supervisor JVM is replaced
once. In external placement Control Center alone is replaced while the exact
external Supervisor and target stay live. After lookup and retry checks,
the external Supervisor alone is replaced while the reconstructed Control
Center remains live. Target TCP readiness is checked across each boundary.

After each actual readiness boundary, protected original request lookup and
the exact same Start retry must retain the original request, Supervisor and
instance IDs. The same running instance must be present in the actual runtime
projection with TCP readiness, and the safe audit identity set and records must
remain unchanged. Product recovery uses only its existing validated native
evidence path. Contrary product behavior fails with the original error.

Given all selected reconstruction boundaries have passed, perform inherited
idempotent Restart and Stop. Restart must acquire an instance different from
the retained Start. Stop must match that restarted instance; exact retries,
one-record audit additions and target port reuse must pass before clearing
the possibly-started flag. Only then may the placement set `clients=true`.
This is graceful JVM reconstruction, independent of the consumed deliberate
failure authorization.

## Bounds, exit and retained evidence

Each placement has a maximum 900-second active deadline. The overall owned
session reserves 400 seconds of its 2100-second limit for inherited cleanup;
active work stops after at most 1700 seconds. Inherited HTTP 25-second,
observation 35-second, startup 90-second, TERM eight-second, KILL five-second
and port-reuse ten-second bounds are preserved. There are no automatic reruns.

Every acquired Control Center, Supervisor, Chrome and Node group remains in
the inherited process ledger. `finally` attempts protected Stop whenever
Start is uncertain, restoring exclusion first when necessary. It then attempts
every acquired group with bounded TERM/KILL, direct-child reaping and group
absence, followed by absent-listener and `SO_REUSEADDR`-only bind checks for
every case port. Pending signals and the primary error remain distinct from
cleanup diagnostics; signal handlers preserve mandatory cleanup.

Initial and final aggregate receipts retain inherited `readiness`, `clients`,
`cleanup`, `processCompletion`, `success`, `error`, `cleanupDiagnostics` and
`runDirectory` shape. The actual acquired `processDirectory/server-process.json`
contains PID, start timestamp, argv, Control Center URL and Supervisor URL;
Control Center reconstruction updates it to the actual replacement. Normal
PASS requires both placements, all connected browser/recovery scenarios,
complete cleanup and no primary error. A failure remains nonzero even when
cleanup succeeds. Reports, HTTP traces, process logs, profiles and screenshots
remain beneath ignored `target/phase-4-packaged-operations/case-*` outputs and
are never committed.

This route supplies generated-key behavioral acceptance, not a proof of every
interleaving. A run with Java 25 and explicit development source inputs earns
only that Java 25/source-graph boundary. Java 17, the default dependency graph,
full affected suites, final Phase validation and the sole comprehensive Phase
review remain separate obligations. Existing catalog, registration, Launcher,
SPI, security and evidence semantics are consumed unchanged.
