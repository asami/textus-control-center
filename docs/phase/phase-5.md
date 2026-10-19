# Phase 5: Summary API and First Menu Bar Launch

status=in-progress
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-deferred
aggregate_validation_owner=PHASE-5.1
aggregate_validation_sequence=["PHASE-5","PHASE-5.1"]
depends_on=phase-4.md
successor=phase-5.1.md
Reconciled: 2026-10-18
Checklist: [Phase 5](phase-5-checklist.md)
Design: [Menu Bar and Operational View](../design/macos-menu-bar-and-operational-view.md)
Handoff: [Development instructions](../journal/2026/10/2026-10-18-phase-5-main-route-plan-handoff.md)
First-route result: [Operator and successor handoff](../journal/2026/10/2026-10-19-phase-5-first-route-handoff.md)

## Goal

Deliver actual Control Center state through a common Summary Operation/HTTP API
to a thin Swift/SwiftUI Menu Bar app, open the Dashboard and demonstrate the
first supported CAR installation, launch and intended login integration.

## Retained starting point

Reuse Phase 4's operational-component/catalog identities, Launcher safe evidence
projection, Supervisor-owned lifecycle boundary, operation-backed Web panel and
accepted bounded Job Audit reads. Phase 4 remains closed; its original commits,
accepted tests/reviews, failed legacy validation and nonblocking follow-ups retain
their recorded disposition. They do not establish the new Summary API, Swift app
or packaging acceptance. Preserve current work and judge evidence applicability
at the affected boundary instead of restarting the old Phase.

## Work and closure boundary

### Validation method amendment — 2026-10-19

The user directed that progress take priority over automating the native UI.
Native UI automation is no longer a Phase 5 acceptance requirement. Stop the
AX diagnostic work and automated menu/button retries. Retain the existing
executable specifications, actual HTTP, CAR installation, launch, login and
cleanup evidence at their demonstrated scopes. Keep all failed automation
results as failures; they are not converted into passing product checks.

The manual smoke check for the normal packaged app's Summary and actual
Open Dashboard button passed in r22. The human confirmed the three requested
Summary values matched the actual API and that Open Dashboard opened the
browser. Exact button-feedback wording was not separately reported and is not
inferred. No synthetic UI action or AX traversal is required. Preserve
independent Step and Phase reviews and the existing release/commit gates.
Incomplete native AX diagnostic additions are already parked privately and
excluded from this delivery. Original Goal, bases, epoch, repair counters and
failure history remain unchanged.

| Order | Owned checklist | Required outcome |
| --- | --- | --- |
| 1 | CC5-01 | Reconcile existing operational/catalog/CNCF/Launcher inputs. Freeze actual Summary signatures, target/instance identity, overall health, unavailable/not-applicable semantics, URL/access and bounded refresh. Select a host/profile and first parent-bundled or separate-CAR path from actual producer support. |
| 2 | CC5-02 / CC5-03 | Actual status/version/uptime/health/runtime summary/Dashboard URL reaches the native app through normal Operation/HTTP JSON. Summary, refresh, disconnection and Open Dashboard work with meaningful changed-contract specs. |
| 3 | CC5-04 | Package the same built .app and demonstrate real installation/launch and intended launcher/login integration through the selected first route. Build-only, fixture-only or mock-only results do not satisfy acceptance. |
| 4 | CC5-A1 / CC5-A2 | Complete focused/affected regression validation, independent Phase review, Step acceptance, setup/host limitations, closure ledger and release; hand off explicit deferred complete validation to Phase 5.1. |

An operator on the recorded supported macOS host can use that packaged app and
normal API. The app distinguishes disconnection from the last observed service
state; bounded absent observations do not become fake healthy/stopped state.
Monitoring/business logic stays in Control Center; clients do not copy service
discovery or health interpretation. Use stable authenticated Operation/REST
boundaries as required; no HTML scraping or private Dashboard JSON.
Reuse stable low-cardinality runtime summaries; no new runtime registry or
collection platform. Preserve existing lifecycle authority and read-only native UI.

## Current first-route result — 2026-10-19

The normal build r3 passed 21 Swift specifications, release compilation and
strict ad-hoc development signature verification. Normal parent-bundled CAR r4
carried that app for the selected macOS 13+ / Darwin arm64 local Operator
profile. Actual r22 staging/private installation, enabled login registration,
detached launch, installed production Summary client and sixteen-second
liveness passed. HTTP checks returned Summary 200, invalid bearer 403, invalid
limit 400 and Dashboard 200.

The human response, “summary ok, open dashboardで開いた。”, confirmed Version
`0.1.1-SNAPSHOT`, Health (control-center) `ok`, Registered runtimes `0 total;
0 returned`, and actual browser opening. Owned cleanup completed: login was
unregistered, only the new own serverURL key was deleted, the native process
and owned groups/readers ended, and the port was reusable. The run ended with
exit 0, no timeout/signal/forced termination and terminal success. No further
host action is required for this documented result.

See the [durable handoff](../journal/2026/10/2026-10-19-phase-5-first-route-handoff.md)
for producer boundaries, setup links, validation scope and successor limits.
CC5-S2 and Phase 5 remain IN-PROGRESS: protected Step review and acceptance,
the sole epoch-2 Phase full review, release and checklist closure are pending.
Successful route execution is not formal closure. Control Center complete
validation remains `deferred-not-run` to Phase 5.1, which has not started.

## Retained Step and failed-attempt history — 2026-10-18 onward

The following dated progress and pending statements describe their original
attempts. The current result above supersedes their route projections without
changing any original receipt, failure, ID or counter.

CC5-S1 (Summary Operation and projection) is accepted and locally committed as
`2deb1b944edba69e975fe3249228a4b35752fc89`. Its four focused Scala suites passed
36 specifications, actual CAR lint exited successfully, and independent Step
review passed. The shared exact-CLI evidence-storage prerequisite was separately
validated with 47 Python specifications, independently reviewed and committed
as `2fb3d92655de12b786bac445c1d58fb2cfa01bcd` in development-workstation.
These results do not establish HTTP runtime, native, installation or login
acceptance. CC5-S2 connects those remaining parts. No whole checklist item or
Phase acceptance is inferred from the first Step.

CC5-S2 remains in progress. The native application passed 21 Swift specifications,
release compilation and strict development signature verification. CNCF launcher
passed its complete 169 specifications and legacy executable specification;
Textus launcher passed its declared 25 specifications and legacy executable
specification, plus the new 16 installation specifications. One Textus assertion
wording repair converged; the original failure remains recorded.

The user-approved CC5-COZY-ARTIFACT-SCOPE-01 correction passed 56 Python
specifications and independent review before application. It adds only the
selected Phase 5 artifact write scope, preserves the original session/history,
and replaces its execution owner. Normal Cozy packaging produced the parent CAR
containing the built application. The initial real HTTP check exposed Summary
property-name conversion. The bounded fix uses the framework's existing typed
JSON response, preserving the specified business contract and normal envelope.
Its four focused suites passed 37 specifications and packageBin; the regenerated
parent CAR passed real Summary HTTP 200, invalid-bearer 403, invalid-limit 400
and Dashboard HTML 200 checks, with terminal server cleanup. Independent focused
re-review passed without findings. The original failure remains in history.

Actual native installation/launch/login/UI acceptance and the connected Step and
Phase reviews remain outstanding. Automatic approval review rejected the first
native session before execution because its app installation/launch, login-service
registration, own preference mutation, browser opening and Accessibility actions
needed explicit host-side authorization. No native host effects occurred. The
denied request is terminal; any approved attempt uses a fresh request and output
directory. Server-only validation does not establish native acceptance.

The subsequently approved single native attempt passed the real HTTP checks
and normal CAR application staging, then failed before installation: the
verification script captured a whole-user process list larger than its 64KiB
limit. Its original exit1 and incomplete cleanup receipt remain recorded.
Read-only follow-up found the saved script/server/stage PIDs absent. The repair
queries only exact executable-path candidate PIDs, preserving independent
identity and signaling guards, output limits and deadlines. Actual app launch,
login registration and UI evidence remain pending; no success is inferred from
this script repair or the PID observations.

The next explicitly approved single attempt passed candidate discovery and
installed the staged application. The installed login command then crashed
before registration: its own-bundle `UserDefaults(suiteName:)` returned nil,
which the entrypoint force-unwrapped. The actual crash identifies that
initialization, before any origin write or SMAppService call. The original
failed/incomplete receipt is retained; the server terminated and the own
preference-domain read reported no domain. The entrypoint now uses standard
application defaults with the same domain and key. A fresh build passed all
21 Swift specifications, release compilation and strict signature verification.
The actual built command-line entrypoint returned the expected input-contract
error without a crash. This bounded check does not establish installation,
login registration, menu presentation or Dashboard-button acceptance. Normal
Cozy packaging produced the fresh parent CAR containing the repaired app, and
independent prerequisite review found no blockers. The explicitly single r3
attempt is consumed.

The newly approved single r4 attempt used that repaired CAR and passed actual
installation, enabled login registration, detached launch and the installed
production Summary client. The owned application remained alive through the
sixteen-second observation interval. System Events then timed out at the initial
owned-PID query, before menu observation or Dashboard-button activation. The OS
log records that the sending process was not permitted to send the AppleEvent
to System Events. This establishes a host automation permission obstacle, not
native UI correctness or an application defect. No OS setting was changed.
Cleanup recorded the native process absent, own login unregistered and the new
serverURL key deleted, but server-group finalization raised EPERM. The original
cleanup=false/incomplete receipt remains unchanged. A separate read-only check
found all four saved script/server/native/AppleScript PIDs absent and no listener
on the selected port. Those later observations do not turn cleanup into PASS.
The one r4 attempt is consumed. A subsequent server-only diagnostic reproduced
EPERM for an exited, unreaped owned child. The harness now polls/reaps its
acquired process handle before signaling the retained group, and still requires
group absence and complete output-reader finalization. The actual exited-child
regression and real HTTP/idle/server cleanup passed; independent prerequisite
review accepted that behavior. The diagnostic runners did not retain all
requested original platform objects; this evidence limitation remains recorded.
The original r4 exception lacked a traceback, so its exclusive cause is not
established and its failed receipt is unchanged.

Deleting the sole new serverURL key left a valid empty own-domain plist. The
verification precondition now permits an absent file or empty dictionary while
rejecting nonempty, non-dictionary and malformed data. Eight actual-helper cases
and a read-only check of that empty host plist passed without host writes;
independent prerequisite review found no blockers.
This does not establish native UI acceptance. Host automation permission still
needs resolution before any separately authorized additional native attempt.
CC5-S2, menu presentation and Dashboard-button acceptance remain open.

After the user reported enabling ChatGPT's System Events switch and expressly
authorized one additional attempt, r5 again passed normal staging, private
installation, enabled login, detached launch, installed Summary client and
sixteen-second liveness. The initial owned-PID System Events query timed out
before any menu observation or Dashboard-button action. Unlike r4, r5 completed
owned cleanup successfully: native absence, login unregistration, deletion of
the newly created serverURL key, server/group finalization and reusable port.
Its actual result remains readiness=true, clients=false, cleanup=true,
terminal=terminal, success=false; completion admission rejected acceptance.
The single r5 authorization is consumed. No automatic retry is admitted.

Scoped r5 OS logs attribute the request to `/Applications/ChatGPT.app`, bundle
identifier `com.openai.codex`, and record its indirect AppleEvents request.
The initially selected window did not establish the final permission reply or
an explicit denial. The user subsequently confirmed Allow. A further scoped
query records access-result1 at15:23:04.320 JST, after the query timeout and
native cleanup by15:22:12.370. Preserve the original failed r5 receipt; this
late response does not establish UI correctness or Accessibility permission.
Fresh original render/submission/poll/admission records were
retained. An earlier preparation-only record loss and a late runner-resume
disclosure remain explicit process failures; neither is reconstructed or erased.
UI/Dashboard evidence, connected Step acceptance, Phase review and release are
still outstanding. The original Goal, bases, counters and failures are preserved.

The next expressly approved single attempt, r6, passed the same real HTTP,
normal parent-CAR staging/private installation, enabled login, detached launch,
installed production Summary client and sixteen-second liveness checks. The
owned UI query reached Accessibility and failed with -25211 before menu values
or Dashboard activation. Its OS log at16:18:12.051 JST records Accessibility
for `com.openai.codex`, responsible `/Applications/ChatGPT.app`, as Denied
(System Set). The current Accessibility switch state requires human observation;
the previous Automation approval does not establish this separate permission.

r6 recorded own login unregistered, its new serverURL deleted and the native
process absent, but owned child-group finalization raised EPERM. Preserve
readiness=true, clients=false, cleanup=false, terminal=incomplete, success=false.
A later read-only check found the four selected saved PIDs absent and no listener
on the selected port54526. It does not accept cleanup or establish the exact
EPERM instruction/cause; the earlier exited-child repair is not proof of this
failure's cause. The same runner also reported that it had not retained the
original verify/render/submission/initial/poll/admission objects. The saved
execution/route receipts are actual artifacts, but do not replace those missing
objects. That retention failure is recorded without reconstruction or replay.
The r6 one-attempt approval is consumed. No further native run or Step/Phase
acceptance follows; all original counters and failures remain preserved.

## Validation ownership

This is the first member of the serial same-repository sequence
`PHASE-5 -> PHASE-5.1`. Phase 5.1 owns the Control Center repository-complete
`sbt-full-suite` validation once for both members. At this Phase's closure,
record `repository_full_suite=deferred-not-run`, not a full-suite pass.
Focused validation, actual API/native/packaged evidence, independent Phase review,
Step acceptance, closure ledger and this Phase's release commit remain mandatory.
This planning split runs none of them.

The aggregate chain has only this Control Center repository as its Phase base.
Needed CNCF/launcher producer capabilities retain their own authority and
validation; their acceptance is not covered or deferred by a Control Center
receipt. Do not adopt an entire upstream roadmap. Reconcile any newly required
multi-repository Phase-base structure before implementation instead of silently
extending this aggregate policy.

The [durable handoff](../journal/2026/10/2026-10-19-phase-5-first-route-handoff.md)
records Summary signatures/authentication/state/identity/URL/refresh, native
source/build and package contracts, selected first form and host/profile, actual
install/login/launch results, producer boundaries and remaining scope for
[Phase 5.1](phase-5.1.md). Step/Phase acceptance and committed deferred-validation
release evidence remain pending. The same high parent profile is recommended
for the successor; this handoff neither starts it nor accepts its requirements.

## Relocated scope and non-goals

| Original unfinished obligation | Sole owner after split |
| --- | --- |
| CC5-01..04 | Phase 5, this plan and checklist |
| CC5-05..08: configured middleware/Docker/development observations and second CAR form | [Phase 5.1](phase-5.1.md) and its [checklist](phase-5.1-checklist.md) |
| CC5-09..10: integrated regression/full validation, review and complete sequence closure | [Phase 5.1](phase-5.1.md) and its [checklist](phase-5.1-checklist.md) |

First-route acceptance closes only this narrowed Phase. Both CAR distribution
models remain required before Phase 6. Missing provider evidence stays a named
gap, with unrelated ready work allowed to continue. Do not fabricate acceptance.
New native lifecycle controls, configuration editing, log browsing, notifications,
mobile/watch clients and Phases 6..9 operations remain outside this delivery.
The [design](../design/macos-menu-bar-and-operational-view.md) retains the common
host-profile, observation and future presentation intent.

## Applied split — 2026-10-18

Explicit invocation: `$cncf-split-phase 5`, apply mode. Source Goal:
none / no-goal-pre-entry. Pre-split gate evidence on this date was
`SPLIT_REQUIRED`, split_reasons=`[time-bound]`, expected 660m (11h), range
540..900m, target 360m and ceiling 480m. This dated evidence is not the current gate.
The earlier unadopted 5.1/5.2 proposal is corrected to the skill's numbering:
**Phase 5, Phase 5.1, then unchanged Phase 6**. Phase 5 is the first unit, not a
container. Phase 6 and Phase 8 now consume the completed observation scope from
5.1; their feature scope and every later Phase number remain unchanged.

No completed Phase 5 Steps, accepted Slices or checked rows existed to relocate.
Original CC5-01..10 meanings are retained exactly once across the two checklists;
CC5-A1/A2 describe this intermediate member's own assurance and handoff.
Prior Phase 4 history, bases, counters, failures and canonical hygiene stay intact.

Both members are expected 360m at gpt-6.1-sol / high, standard parent mode, with
300..480m ranges. Two closures total 720m versus 660m unsplit: estimated extra
60m review/release/handoff overhead. No comparable measured native/dual-CAR Phase
or price saving is claimed. One final Control Center complete validation limits
duplicate campaigns. Summary/auth/identity and native installation decisions
belong here; configured observation and workflow/journal decisions belong to 5.1.
There is no sufficiently settled complete lower-cost child to isolate.
Incoming forced semantic handoffs: Phase 5 `[]`, Phase 5.1 `[]`; ordinary
review/release is not invented as a forced semantic boundary.
Both children meet the six-hour packing target, so short-child merge/rebalance
exceptions do not apply. Merging restores 660m above the eight-hour ceiling;
profile cost never rejects a feasible merge. No Goal, implementation or commit
is created by adopting this split.

## Structural Phase Plan Gate

Phase Plan Gate: PROCEED
- target: 6h expected; allowed ceiling 8h
- estimate_calibration: no comparable measured native/dual-CAR Phase; low-confidence bottom-up estimate, no historical multiplier
- planning_demand: protected-decision
- recommended_parent_profile: gpt-6.1-sol / high
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: Summary authentication/identity/state contract and native Subcomponent installation/login producer connection
- frozen_profile_transition_handoff: none; consumes accepted Phase 4 evidence at its actual scope
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: expected 360m (6h), range 300..480m (5..8h); 0m from target and 120m below expected ceiling
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: combined expected 660m exceeds the 480m ceiling; no semantic barrier is asserted
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: estimated 60m added review/release/handoff overhead across the sequence; no profile-cost saving asserted; one final complete validation avoids repeating that campaign
- agent_reasoning_mode_policy: default standard; reconsider only at an eligible actual agent launch under its quality-first contract
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 5 on 2026-10-18

Routine implementation/specification/assurance stays with this coherent delivery
route because its contract decisions and actual launch/observation evidence must
close together. Once frozen, bounded worker work can use its suitable registered
profile. No separate short execution Phase or artificial model transition is added.
