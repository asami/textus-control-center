# Textus Control Center Menu Bar

This macOS 13+ SwiftUI application presents the server-owned
[Operational Summary](../../docs/spec/operational-summary.md) through one shared
Swift client. See the [native contract](../../docs/spec/macos-menu-bar.md) for
the full URL, failure, refresh, login and authority semantics.

From the primary repository on a Darwin arm64 development host:

```sh
scripts/build-macos-menu-bar.sh \
  --output-root "$PWD/.codex-workflow/native-menu-bar-build"
```

Choose a fresh output root beneath `.codex-workflow`; the driver rejects an
existing `Textus Control Center.app`. It runs Swift tests, builds the release
executable with Swift tools 5.9 / language mode 5, bundles it and signs/verifies
the bundle ad hoc. It uses Apple frameworks and normal platform caches. No
third-party Swift packages are required. Build work stays under the selected
private output root, including compiler temporary files through its `tmp/`
directory. The resulting bundle is a development artifact; ad hoc
signing does not provide production distribution or notarization.

Set your actual Control Center port explicitly. For example, if your selected
standalone server is at port 8080, the shared one-shot diagnostic is:

```sh
"$PWD/.codex-workflow/native-menu-bar-build/Textus Control Center.app/Contents/MacOS/TextusControlCenterMenuBar" \
  --summary-once --server-url http://127.0.0.1:8080
```

This performs the normal GET with `limit=50`, five-second request/resource
deadlines, redirect refusal and a streaming 1 MiB cap, prints received valid
Summary JSON, and exits nonzero for connection/HTTP/contract errors. It does
not start the UI or persist an observation. The same client serves the app.

To run the development app with that explicit selection:

```sh
open "$PWD/.codex-workflow/native-menu-bar-build/Textus Control Center.app" \
  --args --server-url http://127.0.0.1:8080
```

Normal app launch retains this origin in its own UserDefaults domain,
`org.simplemodeling.textus.controlcenter.menubar`; subsequent normal launches
and login launch use it. Only loopback HTTP with explicit port and root path
is supported, including `[::1]` and `localhost`. There is no guessed default;
missing configuration shows setup instructions. Invalid explicit settings
fail instead of falling back.

Use the existing standalone local Operator profile. The app supplies no
launcher token or new authentication mechanism. Summary remains protected by
the existing server Operator/ApplicationContentManager/System/Internal
admission; a 401/403 is displayed as an HTTP failure. Remote HTTPS/token UI is
Phase 5.1 work.

Refresh starts immediately and repeats fifteen seconds after completion, with
one request at a time including manual refresh. On failure, connection/error
text changes while the last successful server Summary and timestamp remain
visible until quit. Unknown server statuses are shown verbatim. Missing
metadata remains unavailable. Dashboard opens the exact server-relative
`/web/textus-control-center` in the browser with normal Web authentication.
Quit cancels the loop and its active request. There are no server controls.

Once the bundle is installed at the installer-selected location, login setup
and owned-session cleanup use that bundle's executable. For an installation
in `/Applications`, the concrete commands are:

```sh
"/Applications/Textus Control Center.app/Contents/MacOS/TextusControlCenterMenuBar" \
  --register-login --server-url http://127.0.0.1:8080
"/Applications/Textus Control Center.app/Contents/MacOS/TextusControlCenterMenuBar" \
  --unregister-login
```

Registration calls Apple's `SMAppService.mainApp` and prints its actual status.
`enabled` exits 0; `requiresApproval`, `notRegistered` or `notFound` exits 2.
If approval is pending, use macOS System Settings → General → Login Items,
then check/register again from the same installed bundle. API errors exit 1.
Unregistration succeeds only when actual status is `notRegistered`.
The app creates no LaunchAgent plist or separate login registry.

The normal parent-bundled CAR staging, private textus-launcher installation,
enabled login registration, detached app launch, production Summary client and
sixteen-second liveness passed in the r22 human smoke check on the selected
macOS 13+ / Darwin arm64 local Operator profile. Normal build r3 passed 21 Swift
specifications, release compilation and strict ad-hoc signature verification;
normal parent CAR r4 contained that app. Summary 200, invalid bearer 403,
invalid limit 400 and Dashboard 200 passed. The human confirmed the three
requested Summary values matched the API and Open Dashboard opened the browser.
Exact button-feedback wording was not reported. Owned cleanup completed,
including login unregistration, deletion of only the new serverURL key,
native/group/reader termination and reusable port. No further host action is
required for this result. Step and Phase review, acceptance and release remain
pending; see the [first-route handoff](../../docs/journal/2026/10/2026-10-19-phase-5-first-route-handoff.md)
for validation scope and successor limits.

The parent CAR carries the built Presentation Subcomponent; the CNCF launcher
stages its selected Darwin arm64 payload, and the textus-launcher adapter owns
installation and detached launch. The installed application's own executable
registers/unregisters `SMAppService.mainApp`. This does not give the read-only
native UI server-lifecycle authority or add authentication.

The following procedure documents how the completed human check was performed;
it is not a request for another host session.

For the human smoke check, the parent runs
`scripts/test/phase5_native_human.py` with the same eight required argument
pairs as `phase5_native_route.py`, a fresh execution directory and a fresh
request ID; `--http-only` is rejected. A fresh app/server session requires a
new concrete once-only host authorization. The route retains the normal CAR,
installation, login, client and owned-cleanup behavior.

After the sound, click the server-rack menu icon, locate the Summary heading
and its 320-point-high scrolling viewport, and compare the printed Version,
Health (control-center) and Registered runtimes values with the menu.
Manually click Open Dashboard and read the selectable feedback below the
Refresh/Open Dashboard row. `Dashboard opening requested` means that macOS
accepted the request; refusal or URL resolution failure is displayed there.
Inspect whether the browser actually opens the Dashboard at the printed URL,
then report the Summary comparison, action feedback and actual browser result
in chat. An accepted request alone does not satisfy Dashboard acceptance.
You may close the menu to return to chat.
The parent alone records actual feedback in the execution directory's fresh
`manual-observation.json`; the helper waits at most 300 seconds and accepts
only matching, attributable feedback confirming both checks. Missing, invalid
or negative feedback fails the check. This is human-assisted verification.
