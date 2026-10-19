# Phase 5: main-route-first plan and development handoff

Date: 2026-10-18
Status: planning reconciliation only; no execution/acceptance or live-state change

## Decision and documents

The user asks to reconcile the strategy with Phase 5, establish a working main
route first, define completion/checklist conditions, and preserve existing code
and validation. The canonical [Phase 5 plan](../../../phase/phase-5.md) now links
its [completion checklist](../../../phase/phase-5-checklist.md) and existing
[design](../../../design/macos-menu-bar-and-operational-view.md).

The strategy's old Phase 5 health-summary heading omitted the native presentation;
its Phase 6 multi-host lifecycle and Phase 7 general automation headings conflicted
with the detailed plans. The [strategy](../../../strategy/textus-control-center-development-strategy.md)
now uses Phase 5 Menu Bar/Operational View, 6 Integrated Admission Console,
7 Subsystem Availability/Immutable Runtime Operations, 8 Docker Supervisor and
9 Native Host Supervisor. Broad multi-host credential/role management and general
alerting/remediation remain explicitly retained future topics, not newly imposed
Phase 5 requirements. Existing Phase 6/7/8/9 plans are not rewritten or activated.

## Retained baseline

Inspected Phase 4 is closed with twenty accepted Steps, the approved Job Audit
read boundary, final validation and its sole epoch-2 comprehensive review plus
focused closure. Source/history at `f7c0920` and the subsequent main merge
`935bf5c` are reference points, not a requirement to reset to them. At inspection
the checkout had no ordinary working-tree changes. Future edits by the owning
chat must be preserved; use the actual current state at adoption.

Reuse existing `OperationalComponentManagement`, normal CML/Operation/factory
wiring, Launcher safe evidence projection, Supervisor-owned lifecycle, Web panel
and link policy. `OperationalComponentManagementSpec`, Launcher evidence specs,
Supervisor protocol specs and Phase 4 packaged/read-flow evidence retain their
original applicability. No Swift source or dedicated Phase 5 acceptance ledger
was found before this edit. Their absence does not negate accepted Phase 4 work.
No recorded test was rerun or independently recertified here.

Preserve all original successes/failures, bases, repair/review history and
nonblocking follow-up dispositions. In particular, the human-approved exclusion
of failed legacy validation remains history, not a new Phase 5 PASS or gate.
Do not adopt the old nonblocking backlog wholesale as a prerequisite.

## Execution order for the development chat

1. At the normal planning boundary, reconcile available producer inputs and
   freeze the minimum Summary contract (CC5-01). Choose one supported configured
   host and the first parent-bundled or separate-CAR packaging route based on
   actual producer support. Settle signatures, unavailable/not-applicable
   semantics, bounded refresh and normal URL/access handling before callers.
   Use existing Operations, identities and evidence; do not build a new registry.
2. Connect actual state -> common Operation/HTTP result -> thin Swift/SwiftUI
   Menu Bar -> Open Dashboard (CC5-02/03). Add meaningful changed-path tests as
   the route is implemented. No fake health, copied monitoring engine or private
   dashboard protocol; no need to implement every observer before the first UI.
3. Package and launch that same app through the first selected CAR/launcher path,
   including intended login integration (CC5-04). Capture actual API/UI/launch
   evidence and usage steps. Build success alone is not an operator outcome.
4. Extend the same route to named middleware/profile behavior, Docker observation
   and development-CAR state/journal projection (CC5-05/06/07). Preserve service
   instance identity and distinct container/lease/health/reachability state.
   Use profile configuration, not a hostname switch. A host without PostgreSQL
   or OpenTelemetry is not unhealthy; missing expected observations stay explicit.
5. Complete actual packaging/launch through the second distribution model
   (CC5-08), reusing the same client and common API. Identify packaging/launcher
   gaps early, but avoid waiting on unrelated upstream roadmap work. First-route
   acceptance alone does not satisfy both distribution obligations.
6. Reconcile applicable prior and new evidence, complete required regressions/
   repository validation and one integrated independent Phase review, publish
   accurate setup/limits and perform ordinary closure (CC5-09/10). No separate
   review or full-suite cycle per helper, middleware instance or packaging form.

## Scope and evidence discipline

Menu Bar is read-only. Existing Phase 4 lifecycle controls remain available;
new native control buttons, configuration editing, log browsing, notifications
and mobile/watch clients remain outside Phase 5. Development status is an
attributable read projection with deep links, not a sm-workflow control UI or
Phase 6 admission implementation. Docker observation does not imply Docker
socket/control authority or a completed Phase 8 Supervisor deployment.

Use actual normal API/native/packaged execution for the main route and bounded
specs for profile/failure branches. Report which host/profile/distribution form
was actually exercised. Unknown/missing producer/host evidence stays a gap;
do not mark a whole checklist item done or claim cross-host testing from a
configuration fixture. Continue independent ready work while a specific handoff
is missing. A new capability gap belongs to its producer, without an invented
fallback or automatic adoption of its entire roadmap.

Adopt without overwriting concurrent changes, resetting an active Goal/base/epoch,
copying dated counters or retargeting issued requests. Collect in-flight results
under their issued contract and reuse them only where applicable. This planning
edit does not start development, invoke launchers/SBT, change user login settings,
publish a CAR, deploy, commit, push or message another chat.

## Verification boundary

Only documentation alignment, work-ID coverage, reference targets and unchecked
completion status are verified here. Existing implementation and executable
specifications are unchanged. No Phase 5 execution or acceptance is claimed.

Documentation validation: five affected documents and 22 local reference targets
checked; CC5-01..10 each maps to the plan and one unchecked completion row.
Strategy titles match the Phase 5–9 plans, Phase 4 remains closed, and
`git diff --check` passes. No product test or deployment was performed.

## Applied split correction — 2026-10-18

After this reconciliation, the explicit `$cncf-goal-phase 5` entry returned
`SPLIT_REQUIRED` before Goal creation: whole-scope expected 11h (range 9..15h),
above the ordinary 8h ceiling. The user then explicitly invoked
`$cncf-split-phase 5` in apply mode. The current authority is now
[Phase 5](../../../phase/phase-5.md) followed by
[Phase 5.1](../../../phase/phase-5.1.md), then the unchanged Phase 6.
The first unit keeps identity 5; the earlier unadopted 5.1/5.2 proposal does not
define the numbering. The preceding sections retain their pre-split history.

CC5-01..04 remain solely in the [Phase 5 checklist](../../../phase/phase-5-checklist.md).
CC5-05..10 move unchanged and still unaccepted to the
[Phase 5.1 checklist](../../../phase/phase-5.1-checklist.md). Phase 5 adds its own
focused assurance/handoff rows CC5-A1/A2. Both units target 6h, range 5..8h,
gpt-6.1-sol / high, standard parent mode. Added review/release/handoff overhead
is estimated at 1h; no measured native/dual-CAR calibration or profile-cost
saving is claimed. The split reason is solely `time-bound`, with no invented
forced semantic boundary or human approval gate.

The serial Control Center chain persists `final-only` and `sbt-full-suite`:
Phase 5 retains focused validation, independent review, Step acceptance and
release with explicit deferred-not-run complete validation; Phase 5.1 owns the
complete validation for both and the integrated original-scope acceptance.
Producer repository authority and validation remain independent. Phase 6 and
Phase 8 dependency references now follow the completed observation owner 5.1;
their feature scope and all later numbers are unchanged.

This split edits planning documents only and starts no implementation, test,
Goal, login-setting change or child Phase. No commit or publication is made.
Phase 4 accepted history, bases, counters, failures and hygiene dispositions
remain intact. Follow the current Phase plans for static validation and entry;
the earlier five-document/22-reference check is only the prior edit's evidence.
