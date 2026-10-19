# Phase 5.1 Checklist: Configured Operational View and Dual CAR Distribution

Status: PLANNED; all relocated obligations remain unaccepted
Plan: [Phase 5.1](phase-5.1.md)
Predecessor: [Phase 5 ledger](phase-5-checklist.md)
Design: [Operational View and native presentation](../design/macos-menu-bar-and-operational-view.md)

This is the sole Phase 5.1 completion ledger. Split from Phase 5 on 2026-10-18:
original CC5-05..10 move here unchanged and unaccepted; CC5-01..04 remain solely
in the predecessor ledger. Preserve all original meanings and prior Phase 4
evidence/failures at their actual scope.

- [ ] CC5-05: Configured Textus runtime and named middleware observations work.
      PostgreSQL ops/dev remain independent; enabled observability services are
      represented. Disabled/not-applicable services do not become unhealthy.
      Mac mini server and MacBook Air client profile behavior is covered without
      hostname-hard-coded rules; actual host execution limits are recorded.
- [ ] CC5-06: Configured Docker OPS infrastructure is observable without CAR
      semantics. Deployment state, Launcher lease, CNCF health/readiness and
      endpoint reachability stay distinct. Docker running is not health; Compose
      is not a runtime registry. No Docker lifecycle/socket authority is added.
- [ ] CC5-07: A representative development-CAR view consumes actual configured
      sm-workflow state and authoritative Service Bus journal events, correlates
      the selected development target and links to detailed control. Unknown or
      unavailable information is explicit. No duplicate workflow state store,
      admission action or exposed remote Service Bus is introduced.
- [ ] CC5-08: The remaining CAR distribution model is packaged and launched;
      both parent-bundled and separate-CAR forms preserve logical Subcomponent
      identity and share the same client/API. Necessary producer handoffs and
      actual installation/launch results are recorded.
- [ ] CC5-09: Actual API -> Menu Bar -> Dashboard -> packaged launch evidence,
      applicable profile/failure specs and affected regressions pass. Reconciled
      prior results retain their conditions; required full-validation obligations
      are satisfied without duplicate campaigns per service or packaging mode.
- [ ] CC5-10: Required independent Phase review has no unresolved blockers;
      usage/setup, profiles, supported platforms, limitations and successor
      handoff are documented, and ordinary closure/release evidence is recorded.

CC5-09 includes this Phase's final-only/sbt-full-suite responsibility for both
PHASE-5 and PHASE-5.1, with the predecessor's committed deferred-validation
handoff and accepted ancestry verified. CC5-10 includes this Phase's independent
integrated review, Step acceptance, closure ledger and release. Predecessor
closure alone does not satisfy these obligations.

Do not check off partial coverage or substitute fixtures for actual host/
installation results. Both CAR forms must share identity/client/API. Missing
sources or host evidence remain named gaps. This planning split runs no tests,
starts no child Phase and creates no Goal or commit.
