# Phase 5.1: Configured Operational View and Dual CAR Distribution

status=planned
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-final-owner
aggregate_validation_owner=PHASE-5.1
aggregate_validation_sequence=["PHASE-5","PHASE-5.1"]
depends_on=phase-5.md
successor=phase-6.md
Planned: 2026-10-18
Split from: [Phase 5](phase-5.md), explicit apply-mode split on 2026-10-18
Checklist: [Phase 5.1](phase-5.1-checklist.md)
Design: [Menu Bar and Operational View](../design/macos-menu-bar-and-operational-view.md)

## Goal

Extend the accepted Summary/Menu Bar route with configured runtime, middleware,
Docker OPS and development-CAR observations, demonstrate the other CAR
distribution model and close integrated acceptance of the Phase 5/5.1 sequence.

## Predecessor and frozen handoff

Start only after Phase 5 closure. Consume its accepted common Summary signatures,
authentication/state/target/instance/Subcomponent identity, URL and refresh
contract; native source/build/package contract; selected first CAR form and
host/profile; real installation/login/launch evidence; producer revisions and
known gaps; and committed deferred-validation release evidence.
Reuse the same app/API and actual Phase 4 evidence within its original scope.
Do not rediscover those contracts or move detailed producer authority here.

## Work and closure boundary

| Order | Sole owned checklist | Required outcome |
| --- | --- | --- |
| 1 | CC5-05 / CC5-06 | Configured Textus runtime and named middleware observations, including independent PostgreSQL ops/dev and enabled observability, preserve configured/disabled/unknown semantics. Keep Docker deployment, Launcher lease, CNCF health/readiness and endpoint reachability distinct. |
| 2 | CC5-07 | Representative development-CAR summary uses configured actual sm-workflow current state and authoritative CNCF Service Bus journal events, with target correlation and deep links. Missing sources remain unavailable/unknown. |
| 3 | CC5-08 | Package/install/launch the remaining parent-bundled or separate-CAR form, keeping the accepted logical Subcomponent identity and the same native app/API. Both forms have actual results. |
| 4 | CC5-09 / CC5-10 | Reconcile predecessor and current API/native/Dashboard/package evidence, focused/profile/failure regressions, aggregate repository-complete validation, one integrated independent Phase review, setup/limits, Step acceptance, closure ledger and release. |

An operator can observe actual configured state and understand unavailable or
not-applicable observations. Named instances remain distinguishable. Both
distribution forms and intended launcher/login integration must be demonstrated;
do not infer a platform matrix or live host behavior from fixtures alone.
Record actual host/profile/form and unsupported environments.

## Configured host and Docker observations

- Mac mini server profile: observe PostgreSQL `ops` and `dev` independently
  and enabled OpenTelemetry/observability services.
- MacBook Air client profile: disabled PostgreSQL/OpenTelemetry are not failures.
- Profiles are configuration, not hostname tests. Unavailable expected observers
  are unknown/unavailable, not evidence of healthy or stopped service.
- Docker OPS may include PostgreSQL, OpenTelemetry, Prometheus, Grafana, Jaeger
  and cloudflared when configured. Ordinary infrastructure needs no CAR identity.
- Keep deployment/container state, Launcher registration/heartbeat lease, CNCF
  health/readiness and external reachability distinct. Docker/Launcher running
  is not application health. Compose is desired topology, not a runtime registry.
- Observe through admitted configured sources without Docker socket/lifecycle
  authority. Docker lifecycle remains [Phase 8](phase-8.md).

## Development and API boundaries

Use actual configured sm-workflow current state plus authoritative CNCF Service
Bus journal events under the [development operations design](../design/development-operations.md).
Project attributable progress/attention and deep links; detailed development
control and decisions stay with their owners. No duplicate workflow store,
private state engine, remote Service Bus exposure or Phase 6 admission action.
Use the accepted normal authenticated Operation/HTTP contract; no HTML/private
Dashboard scraping or client-owned discovery/health logic.
Missing producers do not justify fake health or dropping a whole checklist
obligation; continue independent ready work while recording the exact gap.

## Validation ownership and closure

Phase 5.1 explicitly owns the one Control Center repository-complete
`sbt-full-suite` validation for **PHASE-5 and PHASE-5.1**. Verify the committed
predecessor's deferred-validation handoff and accepted ancestry, then run the
complete validation on the final bound sequence tree. Earlier focused passes or
a deferred disposition are not a full-suite pass. Bootstrap is not used.
Focused validation, actual execution evidence, independent integrated Phase
review, Step acceptance, closure ledger and this Phase's release remain required.
This split itself runs no tests or release.

This is one serial Control Center Phase-base chain. Needed upstream producers
retain their own authority, executable specs and required repository validation;
the aggregate receipt does not cover them. Reconcile a newly required multi-base
structure before execution instead of weakening this contract.

Closure requires all CC5-05..10 plus accepted predecessor CC5-01..04, both physical
CAR forms with one logical identity/app/API, accurate setup/platform limits and
the final validation/review/release evidence. Hand this integrated result to the
unchanged [Phase 6](phase-6.md). Do not silently replay old nonblocking work,
change accepted Phase 4 outcomes or reuse historical results beyond their scope.

## Non-goals

New native start/stop/restart controls, configuration editing, logs, notifications,
mobile/watch implementations, remote Service Bus exposure, copied workflow state,
admission actions, availability/replacement and Docker/host lifecycle control
remain outside this Phase. The shared design retains future presentation intent.

## Split provenance and cost

Split from Phase 5 by `$cncf-split-phase 5` on 2026-10-18. Order:
Phase 4 -> Phase 5 -> Phase 5.1 -> existing Phase 6; no later number is changed.
This Phase owns original CC5-05..10 once; source CC5-01..04 and completed history
stay with their original owner. The original handoff remains dated history with
an appended split correction.

The expensive decisions here are configured observation state separation and
workflow-state/journal correlation/authority. The predecessor's high profile
and frozen API/packaging handoff do not settle these decisions by themselves.
Use gpt-6.1-sol / high, standard parent mode: no profile switch or reasoning-cost
saving is asserted. Routine observation/specification/assurance work stays with
its decision/acceptance route. Estimated 6h, range 5..8h; the whole split adds
60m review/release/handoff overhead but retains only one complete Control Center
validation. See the source's dated split note for original estimate/calibration.
No child execution or Goal is authorized merely by this document.

## Structural Phase Plan Gate

Phase Plan Gate: PROCEED
- target: 6h expected; allowed ceiling 8h
- estimate_calibration: no comparable measured native/dual-CAR Phase; low-confidence bottom-up estimate, no historical multiplier
- planning_demand: protected-decision
- recommended_parent_profile: gpt-6.1-sol / high
- profile_cost_role: expensive reasoning kernel
- expensive_reasoning_kernel: Configured observation state separation and sm-workflow current-state/Service Bus journal correlation and authority
- frozen_profile_transition_handoff: accepted Phase 5 Summary/native/build/first-form contract and evidence, producer revisions, and committed deferred-validation release; same high profile
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
