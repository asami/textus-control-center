# Phase 5 first-route operator and successor handoff

Date: 2026-10-19
Status: CLOSED; first route, CC5-S1/S2 and Phase 5 accepted

The connected Summary API → macOS Menu Bar → Open Dashboard route passed
through the first normal parent-bundled CAR installation. This is operator
and successor input for the [Phase 5 plan](../../../phase/phase-5.md) and
[checklist](../../../phase/phase-5-checklist.md). Protected Step review and
focused blocker closure passed; CC5-S2 local acceptance commits are Control
Center `adde4135`, CNCF launcher `12f2810b`, Textus launcher `de920d6` and
shared workflow `33db5ae`. The sole epoch-2 comprehensive Phase review passed
without blockers. The approved shared completed-receipt correction then passed
the complete workflow driver and independent focused Phase closure. The local
release closes Phase 5 and its six checklist rows. Phase 5.1 has not started
and its requirements are not accepted by this handoff.

## Supported first route and operator setup

The selected profile is macOS 13 or later, Darwin arm64, development/client
with the existing standalone local Operator admission. The same normal built
`Textus Control Center.app` is a Presentation Subcomponent carried inside
`textus-control-center-0.1.1-SNAPSHOT.car`. The first exercised form is
parent-bundled; separate-CAR distribution is still successor work.

Use the [native README](../../../../native/macos/README.md) for build, explicit
server selection, launch and installed-bundle login commands. It describes
the Swift 5.9/language-mode-5 build, fresh private output root and ad-hoc
development signing. That signature is not production signing or notarization.
The [native contract](../../../spec/macos-menu-bar.md) owns client deadlines,
size cap, URL validation, refresh, failure display and login semantics.

Select a loopback HTTP origin with an explicit port and root path. There is
no guessed server; the README's `http://127.0.0.1:8080` is an operator example,
not a service established by this handoff. The completed session used
`http://127.0.0.1:61818` and opened its `/web/textus-control-center` Dashboard;
that session has ended. Retained origin settings support later normal/login
launches. Existing Summary admission requires ApplicationContentManager,
Operator, System or Internal; launcher-registration credentials alone do not
authorize it. The native app adds no token store or permission bypass. Normal
Web authentication applies when the browser opens the Dashboard.

## Producer and consumer interfaces

| Owner | Interface and responsibility |
| --- | --- |
| Control Center | `OperationalSummary.getSummary(GetSummary): GetSummaryResult`, a normal CML QUERY; automatic REST `GET /rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary?limit=50`. The [Summary contract](../../../spec/operational-summary.md) defines exact camelCase JSON, admission, identity, metadata, health scope, safe bounded runtime projection and relative Dashboard URL. |
| CNCF | Existing typed `OperationResponse.Json` preserves the business JSON through normal HTTP/envelope handling. Current Component health, normal `meta.version`, execution-context ancestry and existing registry reads supply the Summary. Launcher staging selects the parent CAR's Darwin arm64 native payload and declared executable. |
| textus-launcher | The native installer adapter consumes that staged payload, installs the bundle, invokes its login command and requests detached launch. It does not move server lifecycle authority into the native UI. |
| Installed native app | One production Swift client serves both the UI and `--summary-once`. `--register-login` / `--unregister-login` use Apple's `SMAppService.mainApp` from the installed bundle. Refresh and Open Dashboard remain read-only presentation actions. |

Control Center owns health interpretation and registry/lease projection;
the client displays those results without discovery or a second health engine.
Component and instance identity are attributable to the actual server.
Unavailable metadata remains unavailable, overall health is scoped to Control
Center, and a runtime lease is not remote runtime health. The response directs
bounded fifteen-second refresh and same-origin Dashboard resolution. See the
linked contracts and their executable specifications for complete semantics.

## Actual result and validation scope

Normal build r3 passed all 21 Swift specifications, release compilation and
strict ad-hoc signature verification. Normal CAR r4 packaged that app through
the parent-bundled route. Actual r22 passed staging, private installation,
enabled login registration, detached launch, installed production Summary
client and sixteen-second liveness. HTTP checks returned Summary 200, invalid
bearer 403, invalid limit 400 and Dashboard HTML 200.

The human was asked to compare these three displayed values against the actual
API and then use Open Dashboard:

- Version: `0.1.1-SNAPSHOT`
- Health (control-center): `ok`
- Registered runtimes: `0 total; 0 returned`

The actual reply, “summary ok, open dashboardで開いた。”, confirmed the
comparison and actual browser opening. Exact action-feedback wording was not
separately reported and is not inferred from that reply. This confirms the
requested three-value comparison, not every displayed field or every failure
branch through human observation.

The session ended exit 0 without timeout, signal or forced termination, with
readiness, clients and cleanup successful and terminal completion. Cleanup
unregistered the own login service, deleted only the newly created own URL key,
ended the exact native process and owned groups/readers, and left the port
reusable. No cleanup diagnostics remained. Local execution receipts stay
private; this document is not an evidence-package export or an instruction to
replay a host session.

Prior validation retains its original scope:

| Boundary | Recorded passing scope |
| --- | --- |
| CNCF launcher | Complete 169 specifications, 15 focused specifications and the legacy executable specification. |
| Textus launcher | Declared 25 specifications, 16 focused installation specifications and the legacy executable specification. |
| Control Center | 37 focused specifications and `packageBin` for the HTTP JSON correction; CC5-S1's accepted 36-specification/CAR-lint result retains its earlier scope. |
| Native | 21 Swift specifications, normal release build and strict development signature verification, followed by the actual connected r22 route above. |

These are not interchangeable repository-wide acceptance receipts. Only the
Control Center repository-complete suite is `deferred-not-run` to Phase 5.1
under the serial `PHASE-5 → PHASE-5.1` final-only/full-suite policy. No Control
Center full-suite pass is claimed, and producer validation is not deferred
under that policy.

## Retained history, limits and successor input

The [Phase 5 ledger](../../../phase/phase-5-checklist.md) retains earlier
permission, initialization, cleanup, automation and observation failures at
their original scopes. Successful r22 does not rewrite those receipts, missing
evidence, consumed approvals or process failures. Original Goal/bases, epoch2,
Step7/Phase0, review35 and all repair counters remain unchanged. Incomplete AX
diagnostic additions are already parked privately and excluded. Automated
native UI traversal is no longer an acceptance requirement.

The exercised host/profile and parent-bundled form do not establish other
architectures, remote HTTPS/authentication, production distribution/signing,
notarization or actual login after reboot. Enabled registration and detached
launch are the demonstrated login/launch scope. A zero-runtime result does
not establish configured middleware or remote health.

[Phase 5.1](../../../phase/phase-5.1.md) and its
[checklist](../../../phase/phase-5.1-checklist.md) retain CC5-05..10: configured
middleware/profile, Docker and development-CAR observations, the second CAR
form, integrated regression/full validation and complete sequence closure.
Missing observations must remain explicit rather than becoming healthy or
stopped. Both distribution forms remain required before Phase 6. New native
lifecycle controls and later Phase operations are outside this route. This
handoff neither starts that work nor accepts any successor obligation.
