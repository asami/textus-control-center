# macOS Menu Bar

## Role and supported profile

The SwiftUI Menu Bar application is a read-only presentation of the normal
[Operational Summary](operational-summary.md). It requires macOS 13 or later;
the initial build/distribution target is Darwin arm64. Control Center owns
health, runtime lease state, registry ordering and counts. The native client
does not discover servers, recompute health, or control server lifecycle.

The first development/client profile accepts an explicitly selected loopback
HTTP origin with an explicit port: `127.0.0.1`, `[::1]`, or `localhost`. The
path must be empty or `/`; credentials, query, fragment, remote hosts, HTTPS
and implicit ports are rejected. There is no default origin. `--server-url
URL` selects and retains the origin in this application's UserDefaults domain,
`org.simplemodeling.textus.controlcenter.menubar`. Normal later launch reads
that retained origin. Invalid explicit arguments or invalid retained settings
are errors without fallback. A fresh launch without an origin displays setup
instructions and Quit.

The client uses the existing standalone local Operator admission contract.
The server still requires an authenticated ApplicationContentManager,
Operator, System or Internal caller. Launcher-registration credentials alone
do not authorize Summary. The native application adds no token storage,
authentication provider or permission bypass. Remote HTTPS/authentication and
configured middleware observers belong to Phase 5.1.

## Wire and observation contract

The exact request is:

```text
GET /rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary?limit=50
Accept: application/json
```

An ephemeral URLSession has five-second request and resource deadlines,
disables persistent cookies/cache and refuses redirects. Only 2xx responses
are admitted. Response accumulation stops at 1 MiB; larger responses fail.
The decoder consumes the normal Summary JSON object directly. It rejects
unsupported protocol versions, malformed timestamps, inconsistent
available/unavailable metadata, inconsistent counts, noncanonical Dashboard
paths or refresh intervals. Optional unavailable metadata values and optional
runtime entry details are omitted. An unavailable uptime never becomes zero.
Unknown server status/health strings remain verbatim.

One shared production client serves the menu bar and `--summary-once`. The
one-shot mode prints the received Summary JSON after successful decoding and
exits without starting SwiftUI; errors exit nonzero. Conflicting modes,
unknown arguments and missing argument values fail.

The monitor starts refreshing immediately, then waits fifteen seconds after
each completion. Automatic and manual refreshes cannot overlap; a request
arriving during an active refresh is coalesced into that active observation.
Its cancellable loop and active request stop on application termination.
Cancellation does not invent a failed server observation.

The view displays connection status, last server `observedAt`, Control Center
status/version/uptime/runtime version, health with scope/checks, runtime
total/returned/truncation, all four state counts and returned entries.
Unavailable metadata is explicit. HTTP, connection and contract failures
remain visible while the last successful Summary and observedAt are retained
in memory. No successful Summary persists across application restarts. The
menu label shows disconnection separately from last observed health.

A populated Summary has a visible `Summary` heading in subheadline styling,
followed by a 320-point-high vertically scrolling viewport. The menu retains
its 410-point width and the existing field order.

Open Dashboard uses NSWorkspace and resolves only the exact relative
`/web/textus-control-center` against the selected origin. Browser/Web
authentication remains in force. Refresh, Open Dashboard and Quit are the
only operational actions.

Below the Refresh/Open Dashboard row, selectable caption text reports the
latest Dashboard action result. When NSWorkspace accepts the request, it
shows `Dashboard opening requested: <absolute validated URL>`; when NSWorkspace
rejects it, it shows `Could not open Dashboard in the default browser:
<absolute validated URL>`. URL resolution failure shows `Could not resolve
Dashboard URL.`. Request acceptance does not establish that the browser opened
or rendered the Dashboard; actual browser observation remains required.

## Login and packaging contract

`--register-login` runs `SMAppService.mainApp.register()` from the installed
app bundle and reads actual status. It prints `enabled`, `requiresApproval`,
`notRegistered` or `notFound`; only enabled exits successfully. Pending OS
approval is explicit and exits 2. `--unregister-login` uses the same owned
service to clean up registration and succeeds only at `notRegistered`.
Invocation failures exit 1. Both modes may be supplied `--server-url URL` to
retain the explicit origin used by subsequent login launches. Login commands
cannot be combined with each other or with `--summary-once`. No LaunchAgent
plist, privileged helper, separate login registry or credential store exists.

The repository build driver accepts `--output-root ABS`, confined beneath the
primary repository's private `.codex-workflow` directory after existing-parent
normalization. It rejects symlink escapes and preexisting app destinations,
uses normal Swift platform caches, runs Swift specifications then a release
build for `arm64-apple-macosx13.0`, and bundles that same executable in
`Textus Control Center.app`. Bundle ID is the UserDefaults domain above;
`LSUIElement` is true, minimum macOS is 13, bundle version is 1, and display
version is 0.1.1. The repository's 0.1.1-SNAPSHOT coordinate is unchanged.
The driver signs the whole bundle ad hoc and verifies that signature. It
does not launch the app/server, register login, download or publish anything.

This ad hoc bundle is for development distribution. Production signing,
notarization and remote/authenticated profiles are outside this slice.
The parent-bundled CAR and textus-launcher installer adapter remain producer
work: this document does not claim implemented installation, native runtime,
HTTP or login acceptance. The connected Step must verify those actual paths.

Build and setup commands are in the [native README](../../native/macos/README.md).
Executable specifications are
[SummaryContractSpec](../../native/macos/Tests/ControlCenterClientTests/SummaryContractSpec.swift)
and [SummaryMonitorSpec](../../native/macos/Tests/ControlCenterClientTests/SummaryMonitorSpec.swift).

## Presentation diagnostic preparation

`CC5_UI_DIAGNOSTICS` is an opt-in compile definition. Normal builds exclude
the diagnostic implementation, calls, view modifiers and added lifecycle
callbacks. Diagnostic builds retain the existing application entry, monitor,
Summary, Dashboard and login behavior.

The helper emits at most 64 notice records per process through OSLog subsystem
`org.simplemodeling.textus.controlcenter.menubar`, category `cc5-presentation`.
Events are fixed: `entry`, `applicationDispatch`, `applicationInitialized`,
`willFinishLaunching`, `didFinishLaunching`, `didBecomeActive`,
`didResignActive`, `contentAppeared`, `contentDisappeared` and `willTerminate`.
Each record contains only that event, fixed role `production` or `minimal`,
the own process ID, main-thread flag, whether NSApp exists, activation-policy
raw integer, active flag and total/visible own-application window counts.
Application-dependent fields are `unknown` when NSApp is absent. Every
interpolation uses explicit public privacy; no URL, path, content,
configuration, credentials or error text is recorded. Later calls after the
cap are ignored. Reading optional NSApp does not create an application or
activate or present UI. The helper creates no timers, network activity, file
output or monitor mutations.

The standalone SwiftUI fixture uses `CC5_MINIMAL_REPRO` with the same helper,
a constant MenuBarExtra and content, and its own Quit action. Its bundle ID
is `org.simplemodeling.textus.controlcenter.cc5.minimal`. It has no HTTP,
client, monitor, preferences, login integration or command parsing.

The build-only diagnostic command is:

```sh
scripts/test/build_phase5_presentation_diagnostics.sh --output-root /ABSOLUTE/REPOSITORY/.codex-workflow/phases/PHASE-5/presentation-diagnostics-build-r1
```

It requires Darwin arm64 and exactly one absolute output root, confined after
existing-parent normalization to a fresh strict descendant of the canonical
existing `.codex-workflow/phases/PHASE-5` directory. Dot components, symlink
escapes and preexisting output destinations are rejected. It uses a private
temporary directory there and normal Apple SDK/SwiftPM caches, runs the
unchanged default Swift specifications, compiles a diagnostic production app
and the standalone fixture, then ad hoc signs and verifies both private
bundles. It prints their paths without launching an app or server or changing
login, preferences or CARs.

These assets prepare diagnostic observations and make no runtime or production
acceptance claim. The minimal fixture does not replace actual Summary UI,
Dashboard, CAR, installation or login acceptance. Logs may be dropped or
unavailable; absence and asynchronous event order are not root-cause proof.
